// TikTok Scraper v6.1 - Trackers (Interaction, Search, Profile, WatchTime)
// ============================================================================

'use strict';

// ============================================================================
// WATCH TIME TRACKER
// ============================================================================

class WatchTimeTracker {
    constructor(state, messenger) {
        this.state = state;
        this.messenger = messenger;
        this.videoMetadata = new Map();  // videoId -> { authorHandle, videoUrl }
    }
    
    /**
     * Store video metadata for later use when sending watch time
     */
    setVideoMetadata(videoId, authorHandle, videoUrl) {
        if (!videoId) return;
        this.videoMetadata.set(videoId, { authorHandle, videoUrl });
    }
    
    /**
     * Start tracking watch time for a video
     * Called when video becomes visible/loaded
     */
    startWatching(videoId) {
        if (!videoId || !this.state.isActive) return;
        
        // If already watching this video, don't restart
        if (this.state.currentWatchVideoId === videoId && this.state.watchStartTime) {
            return;
        }
        
        // Stop tracking previous video first (this will send the watch time)
        if (this.state.currentWatchVideoId && this.state.currentWatchVideoId !== videoId) {
            this.stopWatching();
        }
        
        this.state.currentWatchVideoId = videoId;
        this.state.watchStartTime = Date.now();
        
        console.log('[WatchTime] Started tracking:', videoId);
    }
    
    /**
     * Stop tracking and send the watch time event
     * Called when user scrolls to a different video
     */
    stopWatching() {
        if (!this.state.currentWatchVideoId || !this.state.watchStartTime) {
            console.log('[WatchTime] stopWatching called but no active video');
            return null;
        }
        
        const videoId = this.state.currentWatchVideoId;
        const watchDuration = Date.now() - this.state.watchStartTime;
        
        console.log('[WatchTime] Stopping:', videoId, 'Duration:', watchDuration, 'ms');
        
        // Only send if watched for at least 500ms (filter out quick scrolls)
        if (watchDuration >= 500) {
            const metadata = this.videoMetadata.get(videoId) || {};
            
            const watchData = {
                video_id: videoId,
                video_url: metadata.videoUrl || `https://www.tiktok.com/video/${videoId}`,
                author_handle: metadata.authorHandle || 'unknown',
                watch_duration_ms: watchDuration,
            };
            
            console.log('[WatchTime] Sending watch data:', watchData);
            
            // Send watch time event
            this.messenger.sendWatchTime(watchData);
            
            console.log('[WatchTime] Sent:', videoId, '- Duration:', watchDuration, 'ms');
        } else {
            console.log('[WatchTime] Skipped (too short):', videoId, '-', watchDuration, 'ms');
        }
        
        // Reset current tracking
        const result = { videoId, watchDuration };
        this.state.currentWatchVideoId = null;
        this.state.watchStartTime = null;
        
        return result;
    }
    
    /**
     * Get the current watch time for the active video
     */
    getCurrentWatchTime() {
        if (!this.state.currentWatchVideoId || !this.state.watchStartTime) {
            return 0;
        }
        return Date.now() - this.state.watchStartTime;
    }
    
    /**
     * Cleanup on scraper stop - send any pending watch time
     */
    cleanup() {
        this.stopWatching();
        this.videoMetadata.clear();
        this.state.videoWatchTimes.clear();
    }
}

// ============================================================================
// INTERACTION TRACKER
// ============================================================================

class InteractionTracker {
    constructor(state, messenger, extractor, contextDetector = null) {
        this.state = state;
        this.messenger = messenger;
        this.extractor = extractor;
        this.contextDetector = contextDetector;
        
        // Cache video metadata for richer interaction events
        this.videoMetadataCache = new Map();
    }
    
    getButtons(article) {
        if (!article) return { like: null, save: null };
        
        let likeButton = null;
        let saveButton = null;
        
        // PRIORITY 1: Semantic selectors (most stable)
        // These use data-e2e attributes which TikTok uses for testing
        likeButton = article.querySelector('[data-e2e="like-icon"]')?.closest('button');
        saveButton = article.querySelector('[data-e2e="undefined-icon"], [data-e2e="favorite-icon"]')?.closest('button');
        
        // PRIORITY 2: ARIA labels (language-dependent but more reliable than position)
        if (!likeButton) {
            const ariaLikeBtn = article.querySelector('button[aria-label*="like" i], button[aria-label*="mi piace" i]');
            if (ariaLikeBtn && !ariaLikeBtn.closest('aside')) {
                likeButton = ariaLikeBtn;
            }
        }
        if (!saveButton) {
            const ariaSaveBtn = article.querySelector('button[aria-label*="save" i], button[aria-label*="bookmark" i], button[aria-label*="favorite" i], button[aria-label*="salva" i]');
            if (ariaSaveBtn && !ariaSaveBtn.closest('aside')) {
                saveButton = ariaSaveBtn;
            }
        }
        
        // PRIORITY 3: Position-based fallback (LAST RESORT)
        // CAUTION: This assumes TikTok's button order: like, comment, save, share
        // If TikTok changes button order, this will break. The fallback is isolated
        // here to make it easy to update when DOM structure changes.
        if (!likeButton || !saveButton) {
            const actionButtons = this._findActionButtonsByNumber(article);
            if (actionButtons.length >= 4) {
                // Normal video: like, comment, save, share
                if (!likeButton) likeButton = actionButtons[0];
                if (!saveButton) saveButton = actionButtons[2];
            } else if (actionButtons.length === 3) {
                // Ad video: like, comment, share (no save)
                if (!likeButton) likeButton = actionButtons[0];
                // No save for ads
            } else if (actionButtons.length === 2) {
                // Live video or minimal ad: like, share
                if (!likeButton) likeButton = actionButtons[0];
                // No save for live
            }
        }
        
        return { like: likeButton, save: saveButton };
    }
    
    /**
     * Find buttons that contain numbers (with optional K/M/B suffix).
     * Order: like, comment, save, share for normal videos
     *        like, comment, share for ads
     *        like, share for live
     */
    _findActionButtonsByNumber(article) {
        // Exclude buttons inside aside (comments section)
        const allButtons = Array.from(article.querySelectorAll('button'))
            .filter(btn => !btn.closest('aside') && btn.querySelector('svg'));
        
        const buttonsWithNumbers = [];
        
        for (const btn of allButtons) {
            const text = btn.textContent?.trim() || '';
            // Match numbers with optional K, M, B suffix
            if (/[\d.,]+[KMB]?/i.test(text) || text === '0') {
                buttonsWithNumbers.push(btn);
            }
        }
        
        // Group by parent to find the action bar
        const byParent = new Map();
        for (const btn of buttonsWithNumbers) {
            const parent = btn.parentElement;
            if (parent) {
                const siblings = byParent.get(parent) || [];
                siblings.push(btn);
                byParent.set(parent, siblings);
            }
        }
        
        // Find the largest group (action bar)
        let actionBar = [];
        for (const [, buttons] of byParent) {
            if (buttons.length > actionBar.length) {
                actionBar = buttons;
            }
        }
        
        return actionBar.length >= 2 ? actionBar : buttonsWithNumbers.slice(0, 4);
    }
    
    isButtonActive(button) {
        if (!button) return false;
        
        const ariaPressed = button.getAttribute('aria-pressed');
        if (ariaPressed !== null) return ariaPressed === 'true';
        
        const svg = button.querySelector('svg');
        if (svg && this._hasActiveColor(svg)) return true;
        if (svg && this._isFilledIcon(svg)) return true;
        if (this._hasActiveClass(button)) return true;
        
        const dataState = button.getAttribute('data-state') || button.getAttribute('data-active');
        if (dataState === 'true' || dataState === 'active') return true;
        
        return false;
    }
    
    _hasActiveColor(svg) {
        // Check for TikTok's active state colors
        // Using flexible patterns to handle color variations
        const isActiveRed = (r, g, b) => r > 200 && g < 100 && b < 150; // Red-ish (like)
        const isActiveYellow = (r, g, b) => r > 200 && g > 180 && b < 100; // Yellow-ish (save)
        
        for (const path of svg.querySelectorAll('path, circle, ellipse')) {
            const fill = path.getAttribute('fill') || '';
            const computed = window.getComputedStyle(path).fill || '';
            
            // Try to extract RGB values
            const rgbMatch = (fill + ' ' + computed).match(/rgb\((\d+),\s*(\d+),\s*(\d+)\)/i);
            if (rgbMatch) {
                const [, r, g, b] = rgbMatch.map(Number);
                if (isActiveRed(r, g, b) || isActiveYellow(r, g, b)) {
                    return true;
                }
            }
            
            // Fallback: check for known hex colors
            const hexPatterns = [
                /#FE2C55/i, /#FF3B5C/i, /#E02B50/i,  // Red variants
                /#FFDD33/i, /#FACE15/i, /#FFD700/i,  // Yellow variants
            ];
            const colorStr = fill + ' ' + computed;
            if (hexPatterns.some(p => p.test(colorStr))) {
                return true;
            }
        }
        
        return false;
    }
    
    _isFilledIcon(svg) {
        const path = svg.querySelector('path');
        if (!path) return false;
        
        const hasStroke = path.getAttribute('stroke') && path.getAttribute('stroke') !== 'none';
        const fill = path.getAttribute('fill') || '';
        
        return !hasStroke && fill && fill !== 'currentColor' && fill !== 'none' && /^(#|rgb|hsl)/i.test(fill);
    }
    
    _hasActiveClass(element) {
        let el = element;
        while (el && el !== document.body) {
            if (/active|selected|pressed|filled|liked|saved/i.test(el.className || '')) {
                return true;
            }
            el = el.parentElement;
        }
        return false;
    }
    
    capture(article, platformId) {
        if (!article || !platformId) return;
        if (this.state.videoInteractionStates.has(platformId)) return;
        
        const buttons = this.getButtons(article);
        const isLiked = this.isButtonActive(buttons.like);
        const isSaved = this.isButtonActive(buttons.save);
        
        const timestamp = new Date().toISOString();
        
        // Extract video metadata for richer interaction context
        const authorData = this.extractor.extractAuthor(article, false);
        const authorHandle = authorData?.handle || authorData || 'unknown';
        
        // Build video URL from platformId and author
        const videoUrl = authorHandle !== 'unknown' 
            ? `https://www.tiktok.com/@${authorHandle}/video/${platformId}`
            : '';
        
        // Get current context
        const contextType = this.contextDetector?.detect() || 'unknown';
        const pageUrl = window.location.href;
        
        // Cache video metadata for future interaction events
        this.videoMetadataCache.set(platformId, {
            authorHandle,
            videoUrl,
        });
        
        this.state.videoInteractionStates.set(platformId, {
            isLiked,
            isSaved,
            capturedAt: Date.now(),
        });
        
        // Build normalized interaction events with full context
        const interactions = [
            this._buildInteractionEvent(platformId, 'like', 'initial', isLiked, timestamp, contextType, pageUrl),
            this._buildInteractionEvent(platformId, 'save', 'initial', isSaved, timestamp, contextType, pageUrl),
        ];
        
        this.messenger.sendInteractions(interactions);
        this._attachObservers(article, platformId, buttons);
    }
    
    // Build a normalized interaction event object
    _buildInteractionEvent(platformId, type, action, isActive, timestamp, contextType, pageUrl) {
        const metadata = this.videoMetadataCache.get(platformId) || {};
        
        return {
            // Core identifiers
            video_id: platformId,
            
            // Interaction details
            type: type,
            action: action,
            is_active: isActive,
            
            // Context
            context_type: contextType,
            
            // Timing
            timestamp: timestamp,
        };
    }
    
    _attachObservers(article, platformId, buttons) {
        if (this.state.interactionObservers.has(platformId)) return;
        
        const { like: likeBtn, save: saveBtn } = buttons;
        
        this._attachClickListener(article, platformId, 'like', likeBtn);
        this._attachClickListener(article, platformId, 'save', saveBtn);
        
        const observer = new MutationObserver(() => {
            if (!this.state.isActive) return;
            
            // Check if article was removed from DOM - cleanup if so
            if (!document.contains(article)) {
                this.cleanup(platformId);
                return;
            }
            
            setTimeout(() => {
                if (!document.contains(article)) {
                    this.cleanup(platformId);
                    return;
                }
                this._checkStateChange(article, platformId);
            }, 100);
        });
        
        observer.observe(article, {
            attributes: true,
            childList: true,
            subtree: true,
            attributeFilter: ['aria-pressed', 'fill', 'class', 'd', 'data-state', 'style'],
        });
        
        this.state.interactionObservers.set(platformId, observer);
    }
    
    _attachClickListener(article, platformId, type, button) {
        if (!button) return;
        
        button.addEventListener('click', () => {
            setTimeout(() => {
                if (!this.state.isActive || !document.contains(article)) return;
                
                const currentState = this.state.videoInteractionStates.get(platformId);
                if (!currentState) return;
                
                const stateKey = type === 'like' ? 'isLiked' : 'isSaved';
                const newState = this.isButtonActive(button);
                
                if (newState !== currentState[stateKey]) {
                    const timestamp = new Date().toISOString();
                    const contextType = this.contextDetector?.detect() || 'unknown';
                    const pageUrl = window.location.href;
                    
                    this.messenger.sendInteractions([
                        this._buildInteractionEvent(
                            platformId,
                            type,
                            newState ? 'add' : 'remove',
                            newState,
                            timestamp,
                            contextType,
                            pageUrl
                        )
                    ]);
                    currentState[stateKey] = newState;
                }
            }, CONFIG.timing.INTERACTION_DELAY);
        }, { passive: true });
    }
    
    _checkStateChange(article, platformId) {
        const currentState = this.state.videoInteractionStates.get(platformId);
        if (!currentState) return;
        
        const buttons = this.getButtons(article);
        const newLiked = this.isButtonActive(buttons.like);
        const newSaved = this.isButtonActive(buttons.save);
        
        const interactions = [];
        const timestamp = new Date().toISOString();
        const contextType = this.contextDetector?.detect() || 'unknown';
        const pageUrl = window.location.href;
        
        if (newLiked !== currentState.isLiked) {
            interactions.push(
                this._buildInteractionEvent(
                    platformId,
                    'like',
                    newLiked ? 'add' : 'remove',
                    newLiked,
                    timestamp,
                    contextType,
                    pageUrl
                )
            );
            currentState.isLiked = newLiked;
        }
        
        if (newSaved !== currentState.isSaved) {
            interactions.push(
                this._buildInteractionEvent(
                    platformId,
                    'save',
                    newSaved ? 'add' : 'remove',
                    newSaved,
                    timestamp,
                    contextType,
                    pageUrl
                )
            );
            currentState.isSaved = newSaved;
        }
        
        if (interactions.length > 0) {
            this.messenger.sendInteractions(interactions);
        }
    }
    
    cleanup(platformId) {
        const observer = this.state.interactionObservers.get(platformId);
        if (observer) {
            observer.disconnect();
            this.state.interactionObservers.delete(platformId);
        }
        this.videoMetadataCache.delete(platformId);
    }
    
    cleanupAll() {
        this.state.interactionObservers.forEach(obs => obs.disconnect());
        this.state.interactionObservers.clear();
        this.state.videoInteractionStates.clear();
        this.videoMetadataCache.clear();
    }
}

// ============================================================================
// SEARCH TRACKER
// ============================================================================

class SearchTracker {
    constructor(state, messenger, contextDetector = null) {
        this.state = state;
        this.messenger = messenger;
        this.contextDetector = contextDetector;
        this.lastTrackedQuery = null; // Track the last query to avoid duplicates
    }
    
    checkUrlChange() {
        const currentUrl = window.location.href;
        
        if (currentUrl !== this.state.lastTrackedUrl) {
            this.state.lastTrackedUrl = currentUrl;
            this.state.cachedVideoId = null;
            
            const query = this._extractSearchQuery(currentUrl);
            
            // Only track if we have a new query (different from last tracked)
            if (query && query !== this.lastTrackedQuery) {
                this._trackSearch(query, currentUrl);
                this.lastTrackedQuery = query;
            }
            
            // Reset last query when leaving search page entirely
            if (!query) {
                this.lastTrackedQuery = null;
            }
        }
    }
    
    _extractSearchQuery(url) {
        try {
            const urlObj = new URL(url);
            if (urlObj.pathname.includes('/search')) {
                const rawQuery = urlObj.searchParams.get('q');
                // Return decoded query for consistent comparison
                return rawQuery ? decodeURIComponent(rawQuery) : null;
            }
        } catch {}
        return null;
    }
    
    _trackSearch(query, url) {
        if (!query || !this.state.isActive) return;
        
        const event = {
            type: 'search',
            query: query, // Already decoded
            timestamp: new Date().toISOString(),
            url: url,
        };
        
        this.state.searchHistory.push(event);
        this.messenger.sendSearch(event);
    }
}

// ============================================================================
// PROFILE VISIT TRACKER
// ============================================================================

class ProfileVisitTracker {
    constructor(state, messenger, contextDetector = null) {
        this.state = state;
        this.messenger = messenger;
        this.contextDetector = contextDetector;
        this.extractionTimer = null;
        this.lastProfileHandle = null;
        this.lastTrackedUrl = '';
        this.visibilityHandler = null;
        this.previousUrl = null;  // Track referrer
    }
    
    init() {
        this.visibilityHandler = () => {
            if (document.visibilityState === 'visible') {
                console.log('[ProfileVisitTracker] Tab became visible, checking URL');
                this.checkUrlChange();
            }
        };
        document.addEventListener('visibilitychange', this.visibilityHandler);
    }
    
    checkUrlChange() {
        const currentUrl = window.location.href;
        
        if (currentUrl !== this.lastTrackedUrl) {
            const referrerUrl = this.lastTrackedUrl;  // Previous URL as referrer
            this.lastTrackedUrl = currentUrl;
            
            const profileHandle = this._extractProfileHandle(currentUrl);
            console.log('[ProfileVisitTracker] URL changed. Handle detected:', profileHandle);
            
            if (profileHandle && profileHandle !== this.lastProfileHandle) {
                this.lastProfileHandle = profileHandle;
                console.log('[ProfileVisitTracker] New profile detected:', profileHandle);
                
                clearTimeout(this.extractionTimer);
                this.extractionTimer = setTimeout(() => {
                    this._extractAndTrackProfile(profileHandle, currentUrl, referrerUrl);
                }, CONFIG.timing.DOM_SETTLE_DELAY + 500);
            } else if (!profileHandle) {
                this.lastProfileHandle = null;
            }
            
            this.previousUrl = currentUrl;
        }
    }
    
    _extractProfileHandle(url) {
        try {
            const urlObj = new URL(url);
            const path = urlObj.pathname;
            
            const match = path.match(/^\/@([^/]+)$/);
            if (match) {
                return match[1];
            }
        } catch (e) {
            console.error('[ProfileVisitTracker] Error parsing URL:', e);
        }
        return null;
    }
    
    _extractAndTrackProfile(handle, url, referrerUrl) {
        if (!this.state.isActive) return;
        
        const profileKey = `profile:${handle}`;
        if (this.state.visitedProfiles.has(profileKey)) {
            console.log('[ProfileVisitTracker] Already tracked profile:', handle);
            return;
        }
        
        // Use MutationObserver to wait for profile data to load
        this._waitForProfileDataAndTrack(handle, url, referrerUrl, profileKey);
    }
    
    _waitForProfileDataAndTrack(handle, url, referrerUrl, profileKey) {
        const maxWaitTime = 5000; // 5 seconds max wait
        const startTime = Date.now();
        let observer = null;
        let timeoutId = null;
        
        const checkAndExtract = () => {
            // The data-e2e attributes are directly on the <strong> tags
            const followersEl = document.querySelector('strong[data-e2e="followers-count"]');
            const followingEl = document.querySelector('strong[data-e2e="following-count"]');
            const likesEl = document.querySelector('strong[data-e2e="likes-count"]');
            
            // Get text content directly from strong tags
            const followersText = followersEl?.textContent?.trim();
            const followingText = followingEl?.textContent?.trim();
            const likesText = likesEl?.textContent?.trim();
            
            // Verify elements exist AND contain actual count data (not empty/loading)
            // Check for numeric content (with optional K/M/B suffix)
            const hasFollowers = followersText && /[\d.,]+[KMB]?/i.test(followersText);
            const hasFollowing = followingText && /[\d.,]+[KMB]?/i.test(followingText);
            const hasLikes = likesText && /[\d.,]+[KMB]?/i.test(likesText);
            
            // Data is ready ONLY when all three counts are available with actual values
            const isDataReady = hasFollowers && hasFollowing && hasLikes;
            
            if (!isDataReady) {
                console.log('[ProfileVisitTracker] Waiting for data... Followers:', followersText || 'missing', 'Following:', followingText || 'missing', 'Likes:', likesText || 'missing');
            }
            
            if (isDataReady) {
                console.log('[ProfileVisitTracker] Profile data loaded, extracting...');
                
                // Cleanup observer and timeout
                if (observer) {
                    observer.disconnect();
                    observer = null;
                }
                if (timeoutId) {
                    clearTimeout(timeoutId);
                    timeoutId = null;
                }
                
                // Extract and send data
                const profileData = this._extractProfileData(handle, url, referrerUrl);
                console.log('[ProfileVisitTracker] Extracted profile data:', profileData);
                
                this.state.visitedProfiles.add(profileKey);
                this.state.profileVisitHistory.push(profileData);
                this.messenger.sendProfileVisit(profileData);
                console.log('[ProfileVisitTracker] Tracked profile visit:', handle);
                
                return true;
            }
            
            return false;
        };
        
        // Try immediate check first
        if (checkAndExtract()) return;
        
        // Set up MutationObserver to watch for profile data elements
        observer = new MutationObserver(() => {
            if (!this.state.isActive) {
                observer?.disconnect();
                clearTimeout(timeoutId);
                return;
            }
            
            checkAndExtract();
        });
        
        // Observe the entire document body for changes
        // Focus on subtree and childList to catch dynamic content loading
        // Also watch for characterData changes in case counts update
        observer.observe(document.body, {
            childList: true,
            subtree: true,
            characterData: true,
            attributes: false
        });
        
        // Fallback timeout - send partial data if elements don't appear
        timeoutId = setTimeout(() => {
            console.warn('[ProfileVisitTracker] Timeout waiting for profile data, sending partial data');
            
            if (observer) {
                observer.disconnect();
                observer = null;
            }
            
            // Extract whatever we have
            const profileData = this._extractProfileData(handle, url, referrerUrl);
            this.state.visitedProfiles.add(profileKey);
            this.state.profileVisitHistory.push(profileData);
            this.messenger.sendProfileVisit(profileData);
        }, maxWaitTime);
    }
    
    _extractProfileData(handle, url, referrerUrl) {
        const contextType = this.contextDetector?.detect() || 'profile';
        
        const data = {
            // Profile identifiers
            profile_handle: handle,
            profile_url: url,
            
            // Profile metadata (extracted from page)
            display_name: null,
            follower_count: null,
            following_count: null,
            likes_count: null,
            bio: null,
            profile_link: null,
            is_verified: false,
            
            // Context
            context_type: contextType,
            referrer_url: referrerUrl || null,
            
            // Timing
            visited_at: new Date().toISOString(),
    
        };
        
        try {
            // Extract display name
            const subtitle = document.querySelector('[data-e2e="user-subtitle"]');
            if (subtitle) {
                data.display_name = subtitle.textContent?.trim();
            }
            
            // Extract follower count (directly from strong tag)
            const followerEl = document.querySelector('strong[data-e2e="followers-count"]');
            if (followerEl) {
                data.follower_count = followerEl.textContent?.trim();
            }
            
            // Extract following count (directly from strong tag)
            const followingEl = document.querySelector('strong[data-e2e="following-count"]');
            if (followingEl) {
                data.following_count = followingEl.textContent?.trim();
            }
            
            // Extract likes count (directly from strong tag)
            const likesEl = document.querySelector('strong[data-e2e="likes-count"]');
            if (likesEl) {
                data.likes_count = likesEl.textContent?.trim();
            }
            
            // Extract bio (description)
            const bioEl = document.querySelector('[data-e2e="user-bio"]');
            if (bioEl) {
                const bioText = bioEl.textContent?.trim();
                data.bio = bioText || null; // Use null if empty string
            }
            
            // Extract profile link (external link like Instagram, etc.)
            // The data-e2e="user-link" is directly on the <a> tag
            const linkAnchor = document.querySelector('a[data-e2e="user-link"]');
            if (linkAnchor) {
                data.profile_link = linkAnchor.getAttribute('href')?.trim() || null;
            }
            
            // Check for verified badge
            // Strategy 1: SVG sibling of user-title
            let isVerified = false;
            const userTitle = document.querySelector('[data-e2e="user-title"]');
            if (userTitle) {
                // Check next sibling for SVG
                const nextSibling = userTitle.nextElementSibling;
                if (nextSibling && nextSibling.tagName === 'SVG') {
                    isVerified = true;
                }
                // Also check if SVG is within the same parent
                const parent = userTitle.parentElement;
                if (!isVerified && parent && parent.querySelector('svg')) {
                    isVerified = true;
                }
            }
            
            // Strategy 2: Generic verified SVG patterns (fallback)
            if (!isVerified) {
                isVerified = !!document.querySelector('svg[data-e2e*="verified"], svg[aria-label*="verified" i]');
            }
            
            data.is_verified = isVerified;
            
        } catch (e) {
            console.warn('[ProfileVisitTracker] Error extracting profile data:', e);
        }
        
        return data;
    }
    
    cleanup() {
        clearTimeout(this.extractionTimer);
        this.lastProfileHandle = null;
        this.lastTrackedUrl = '';
        this.previousUrl = null;
        
        if (this.visibilityHandler) {
            document.removeEventListener('visibilitychange', this.visibilityHandler);
            this.visibilityHandler = null;
        }
    }
}
