// TikTok Scraper v6.1 - Observers (Video Pipeline, DOM Observation)
// ============================================================================

'use strict';

// ============================================================================
// VIDEO PROCESSOR
// ============================================================================

class VideoProcessor {
    constructor(state, storage, messenger, extractor, interactionTracker, contextDetector, watchTimeTracker) {
        this.state = state;
        this.storage = storage;
        this.messenger = messenger;
        this.extractor = extractor;
        this.interactionTracker = interactionTracker;
        this.contextDetector = contextDetector;
        this.watchTimeTracker = watchTimeTracker;
    }
    
    capture(article) {
        const videoId = this.extractor.extractVideoId(article);
        if (!videoId) return null;
        if (this.state.processedVideoIds.has(videoId)) return null;
        if (this.state.capturedData.has(videoId)) return null;
        
        const isLive = this.extractor.isLive(article);
        const isAd = this.extractor.isAd(article);
        const isAI = this.extractor.isAI(article);
        const platformId = this.extractor.extractPlatformId(article, isLive);
        
        const authorData = this.extractor.extractAuthor(article, isLive);
        let authorHandle, isVerified;
        
        if (isLive) {
            authorHandle = authorData;
            isVerified = null;
        } else {
            authorHandle = authorData.handle;
            isVerified = authorData.verified;
        }
        
        if (authorHandle === 'Unknown') return null;
        
        this.state.articleRefs.set(videoId, new WeakRef(article));
        
        const desc = this.extractor.extractDescription(article, authorHandle);
        const stats = this.extractor.extractStats(article, isAd, isLive);
        const music = this.extractor.extractMusic(article);
        const effect = this.extractor.extractEffect(article);
        
        let videoUrl = '';
        if (isLive) {
            const href = article.querySelector(CONFIG.selectors.LIVE_LINK)?.getAttribute('href') || '';
            videoUrl = href.startsWith('http') ? href : `https://www.tiktok.com${href}`;
        } else if (platformId) {
            videoUrl = `https://www.tiktok.com/@${authorHandle}/video/${platformId}`;
        }
        
        this.state.sessionSequence++;
        
        if (platformId) {
            this.state.cachedVideoId = platformId;
            
            // Extract and cache the author's avatar fingerprint for author-liked detection
            const authorAvatarUrl = this.extractor.extractAuthorAvatarUrl(article);
            if (authorAvatarUrl) {
                const fingerprint = Utils.extractAvatarFingerprint(authorAvatarUrl);
                if (fingerprint) {
                    this.state.cachedVideoAuthorAvatarFingerprint = fingerprint;
                    console.log('[VideoProcessor] Cached author avatar fingerprint:', fingerprint.substring(0, 8) + '...');
                }
            }
        }
        
        const contextType = this.contextDetector ? this.contextDetector.detect() : ContextType.FOR_YOU;
        
        const data = {
            videoId,
            platformId,
            authorHandle,
            isVerified,
            isAd,
            isLive,
            isAI,
            ...desc,
            ...stats,
            musicName: music.name,
            musicId: music.id,
            musicUrl: music.url,
            effectName: effect.name,
            effectId: effect.id,
            effectUrl: effect.url,
            videoUrl,
            sessionSequence: this.state.sessionSequence,
            capturedAt: Date.now(),
            contextType,
        };
        
        this.state.capturedData.set(videoId, data);
        this.state.pendingProcessing.set(videoId, { status: 'pending', queuedAt: Date.now() });
        
        if (platformId) {
            setTimeout(() => this.interactionTracker.capture(article, platformId), 200);
        }
        
        return data;
    }
    
    processQueue() {
        if (!this.state.isActive) return;
        
        const now = Date.now();
        
        this.state.pendingProcessing.forEach((state, videoId) => {
            if (now - state.queuedAt > CONFIG.timing.STALE_THRESHOLD) {
                this.state.pendingProcessing.delete(videoId);
                this.state.capturedData.delete(videoId);
                this.state.articleRefs.delete(videoId);
            }
        });
        
        for (const [videoId, processState] of this.state.pendingProcessing) {
            if (processState.status !== 'pending') continue;
            if (this.state.processingInFlight >= CONFIG.limits.MAX_CONCURRENT) continue;
            if (this.state.processedVideoIds.has(videoId)) {
                this.state.pendingProcessing.delete(videoId);
                this.state.capturedData.delete(videoId);
                continue;
            }
            
            const data = this.state.capturedData.get(videoId);
            if (!data) {
                this.state.pendingProcessing.delete(videoId);
                continue;
            }
            
            if (data.authorHandle === 'Unknown' && !data.platformId?.length && data.description?.length <= 5) {
                this._cleanup(videoId);
                continue;
            }
            
            processState.status = 'processing';
            this.state.processingInFlight++;
            
            setTimeout(() => this._send(videoId, data), CONFIG.timing.PROCESS_DELAY);
        }
    }
    
    _send(videoId, data) {
        if (!this.state.isActive || this.state.processedVideoIds.has(videoId)) {
            this.state.processingInFlight--;
            return;
        }
        
        const articleRef = this.state.articleRefs.get(videoId);
        const article = articleRef?.deref();
        
        // Refresh stats if article is still in DOM
        if (article && document.contains(article)) {
            Object.assign(data, this.extractor.extractStats(article, data.isAd, data.isLive));
        }
        // Note: If article was garbage collected, we use the stats captured initially.
        // This is acceptable as the data is still valid from capture time.
        
        // Store video metadata for watch time tracking
        if (this.watchTimeTracker && data.platformId) {
            this.watchTimeTracker.setVideoMetadata(data.platformId, data.authorHandle, data.videoUrl);
        }
        
        this.messenger.sendVideo(data);
        
        this.state.processedVideoIds.add(videoId);
        this.storage.queueWrite(videoId);
        
        this.state.processingInFlight--;
        this._cleanup(videoId);
    }
    
    _cleanup(videoId) {
        this.state.pendingProcessing.delete(videoId);
        this.state.capturedData.delete(videoId);
        this.state.articleRefs.delete(videoId);
    }
}

// ============================================================================
// OBSERVER MANAGER
// ============================================================================

class ObserverManager {
    constructor(state, extractor, videoProcessor, searchTracker, profileVisitTracker, watchTimeTracker) {
        this.state = state;
        this.extractor = extractor;
        this.videoProcessor = videoProcessor;
        this.searchTracker = searchTracker;
        this.profileVisitTracker = profileVisitTracker;
        this.watchTimeTracker = watchTimeTracker;
        
        this.intersectionObserver = null;
        this.mutationObserver = null;
        this.processingInterval = null;
    }
    
    init() {
        const threshold = 0.4 + Math.random() * 0.3;
        console.log('[ObserverManager] Initializing with threshold:', threshold);
        
        this.intersectionObserver = new IntersectionObserver((entries) => {
            if (!this.state.isActive) return;
            
            entries.forEach(entry => {
                // Use extractor's robust method to get video ID
                const videoId = this.extractor.extractPlatformId(entry.target, false);
                
                console.log('[Observer] Video visibility changed:', videoId, 'isIntersecting:', entry.isIntersecting);
                
                if (entry.isIntersecting) {
                    this._handleVideoContainer(entry.target);
                    
                    if (videoId) {
                        this.state.cachedVideoId = videoId;
                        // Start tracking watch time for this video
                        if (this.watchTimeTracker) {
                            console.log('[Observer] Starting watch time for:', videoId);
                            this.watchTimeTracker.startWatching(videoId);
                        }
                    }
                } else {
                    // Video scrolled out of view - stop tracking if it was the active one
                    if (videoId && this.watchTimeTracker && this.state.currentWatchVideoId === videoId) {
                        console.log('[Observer] Stopping watch time for:', videoId);
                        this.watchTimeTracker.stopWatching();
                    }
                }
            });
        }, { threshold });
        
        this.mutationObserver = new MutationObserver((mutations) => {
            if (!this.state.isActive) return;
            
            const added = new Set();
            
            for (const m of mutations) {
                if (m.type !== 'childList') continue;
                
                for (const node of m.addedNodes) {
                    if (node.nodeType !== 1) continue;
                    
                    if (node.matches?.(CONFIG.selectors.FEED_ITEM)) {
                        added.add(node);
                    }
                    node.querySelectorAll?.(CONFIG.selectors.FEED_ITEM).forEach(el => added.add(el));
                }
            }
            
            added.forEach(c => this.intersectionObserver?.observe(c));
            this.searchTracker.checkUrlChange();
            this.profileVisitTracker?.checkUrlChange();
        });
        
        const feed = document.querySelector(CONFIG.selectors.FEED_LIST);
        this.mutationObserver.observe(feed || document.body, { childList: true, subtree: true });
        
        document.querySelectorAll(CONFIG.selectors.FEED_ITEM).forEach(c => {
            this.intersectionObserver.observe(c);
        });
        
        this.processingInterval = setInterval(() => {
            this.videoProcessor.processQueue();
            this.searchTracker.checkUrlChange();
            this.profileVisitTracker?.checkUrlChange();
        }, CONFIG.timing.PROCESS_INTERVAL);
        
        this.searchTracker.checkUrlChange();
        this.profileVisitTracker?.checkUrlChange();
    }
    
    _handleVideoContainer(article) {
        if (!this.state.isActive || !article) return;
        if (this.state.pendingCaptures.has(article)) return;
        
        const videoId = this.extractor.extractVideoId(article);
        if (videoId && (this.state.processedVideoIds.has(videoId) || this.state.capturedData.has(videoId))) {
            return;
        }
        
        const observer = new MutationObserver(() => {
            if (!this.state.isActive || !document.contains(article)) {
                this._cleanupPending(article);
                return;
            }
            if (this.extractor.isDataReady(article)) {
                this._scheduleCapture(article);
            }
        });
        
        observer.observe(article, { childList: true, subtree: true, characterData: true });
        
        const timeout = setTimeout(() => {
            this._cleanupPending(article);
            if (this.state.isActive && document.contains(article) && this.extractor.hasAuthor(article)) {
                // Re-check if already processed to prevent race with _scheduleCapture
                const currentVideoId = this.extractor.extractVideoId(article);
                if (!currentVideoId || this.state.processedVideoIds.has(currentVideoId)) {
                    return;
                }
                this.videoProcessor.capture(article);
            }
        }, CONFIG.timing.DATA_LOAD_TIMEOUT);
        
        this.state.pendingCaptures.set(article, { observer, timeout, debounce: null });
        
        if (this.extractor.isDataReady(article)) {
            this._scheduleCapture(article);
        }
    }
    
    
    _scheduleCapture(article) {
        const pending = this.state.pendingCaptures.get(article);
        if (!pending) return;
        
        clearTimeout(pending.debounce);
        pending.debounce = setTimeout(() => {
            if (!this.state.isActive || !document.contains(article)) {
                this._cleanupPending(article);
                return;
            }
            this._cleanupPending(article);
            this.videoProcessor.capture(article);
        }, CONFIG.timing.DOM_SETTLE_DELAY);
    }
    
    _cleanupPending(article) {
        const pending = this.state.pendingCaptures.get(article);
        if (pending) {
            pending.observer?.disconnect();
            clearTimeout(pending.timeout);
            clearTimeout(pending.debounce);
            this.state.pendingCaptures.delete(article);
        }
    }
    
    cleanup() {
        clearInterval(this.processingInterval);
        this.processingInterval = null;
        
        this.mutationObserver?.disconnect();
        this.mutationObserver = null;
        
        this.intersectionObserver?.disconnect();
        this.intersectionObserver = null;
        
        // Stop watch time tracking
        if (this.watchTimeTracker) {
            this.watchTimeTracker.cleanup();
        }
        
        this.state.pendingCaptures.forEach(p => {
            p.observer?.disconnect();
            clearTimeout(p.timeout);
            clearTimeout(p.debounce);
        });
        this.state.pendingCaptures.clear();
    }
}
