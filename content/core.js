// TikTok Scraper v6.1 - Core Classes (State, Storage, Messenger)
// ============================================================================

'use strict';

// ============================================================================
// STATE MANAGER
// ============================================================================

class StateManager {
    constructor() {
        this.reset();
        // Limits for unbounded collections
        this.MAX_PROCESSED_COMMENTS = 5000;
        this.MAX_INTERACTION_STATES = 500;
    }
    
    reset() {
        // Video processing state
        this.processedVideoIds = new Set();
        this.capturedData = new Map();
        this.pendingProcessing = new Map();
        this.articleRefs = new Map();
        this.pendingCaptures = new Map();
        
        // Comment state
        this.processedCommentIds = new Set();
        this.capturedComments = [];
        
        // Interaction state
        this.videoInteractionStates = new Map();
        this.interactionObservers = new Map();
        this.capturedInteractions = [];
        
        // Search state
        this.searchHistory = [];
        this.lastTrackedUrl = window.location.href;
        
        // Profile visit state
        this.visitedProfiles = new Set();
        this.profileVisitHistory = [];
        
        // Context state
        this.currentContext = null;
        this.lastVideoId = null;
        this.lastCreator = null;
        
        // Session state
        this.sessionSequence = 0;
        this.processingInFlight = 0;
        this.isActive = false;
        this.cachedVideoId = null;
        this.cachedVideoAuthorAvatarFingerprint = null;  // For author-liked detection
        this.cachedUsername = 'Unknown';
        
        // Watch time tracking
        this.currentWatchVideoId = null;
        this.watchStartTime = null;
        this.videoWatchTimes = new Map();  // videoId -> total watch time in ms
        
        // Enhanced session tracking (for normalized data model)
        this.sessionId = null;  // UUID generated on session start
        this.sessionStartedAt = null;
        this.interactionSequence = 0;  // Sequence counter for interactions within session
        this.searchSequence = 0;       // Sequence counter for searches within session
        this.profileVisitSequence = 0; // Sequence counter for profile visits within session
        
        // Storage state
        this.pendingStorageWrites = new Set();
        this.storageBatchTimer = null;
    }
    
    loadFromStorage() {
        return new Promise((resolve) => {
            chrome.storage.local.get(['processedVideoIds', 'usernameTester', 'currentSessionId'], (result) => {
                result.processedVideoIds?.forEach?.(id => this.processedVideoIds.add(id));
                this.cachedUsername = result.usernameTester || 'Unknown';
                // Session ID will be generated fresh on each start, but we can resume if needed
                resolve();
            });
        });
    }
    
    // Generate a new session ID (UUID v4)
    generateSessionId() {
        this.sessionId = crypto.randomUUID();
        this.sessionStartedAt = new Date().toISOString();
        this.interactionSequence = 0;
        this.searchSequence = 0;
        this.profileVisitSequence = 0;
        return this.sessionId;
    }
    
    // Get next interaction sequence number
    getNextInteractionSequence() {
        return ++this.interactionSequence;
    }
    
    // Get next search sequence number
    getNextSearchSequence() {
        return ++this.searchSequence;
    }
    
    // Get next profile visit sequence number
    getNextProfileVisitSequence() {
        return ++this.profileVisitSequence;
    }
    
    getStats() {
        return {
            videosProcessed: this.processedVideoIds.size,
            commentsScraped: this.capturedComments.length,
            searchesTracked: this.searchHistory.length,
            profilesVisited: this.profileVisitHistory.length,
            sessionSequence: this.sessionSequence,
            interactionsTracked: this.capturedInteractions.length,
        };
    }
    
    /**
     * Trim unbounded collections to prevent memory bloat in long sessions.
     * Call periodically (e.g., every few minutes).
     */
    trimCollections() {
        // Trim processed comment IDs (keep most recent by converting to array and back)
        if (this.processedCommentIds.size > this.MAX_PROCESSED_COMMENTS) {
            const arr = [...this.processedCommentIds];
            const trimmed = arr.slice(-this.MAX_PROCESSED_COMMENTS);
            this.processedCommentIds.clear();
            trimmed.forEach(id => this.processedCommentIds.add(id));
        }
        
        // Trim interaction states (oldest entries)
        if (this.videoInteractionStates.size > this.MAX_INTERACTION_STATES) {
            const entries = [...this.videoInteractionStates.entries()];
            // Sort by capturedAt timestamp, keep newest
            entries.sort((a, b) => (b[1].capturedAt || 0) - (a[1].capturedAt || 0));
            const toKeep = entries.slice(0, this.MAX_INTERACTION_STATES);
            this.videoInteractionStates.clear();
            toKeep.forEach(([k, v]) => this.videoInteractionStates.set(k, v));
        }
    }
}

// ============================================================================
// STORAGE MANAGER
// ============================================================================

class StorageManager {
    constructor(state) {
        this.state = state;
    }
    
    _isContextValid() {
        try {
            return chrome.runtime && !!chrome.runtime.id;
        } catch (e) {
            return false;
        }
    }
    
    queueWrite(videoId) {
        if (!this._isContextValid()) return;
        
        this.state.pendingStorageWrites.add(videoId);
        
        if (this.state.storageBatchTimer) {
            clearTimeout(this.state.storageBatchTimer);
        }
        
        if (this.state.pendingStorageWrites.size >= CONFIG.storage.BATCH_SIZE) {
            this.flush();
        } else {
            this.state.storageBatchTimer = setTimeout(
                () => this.flush(), 
                CONFIG.storage.BATCH_TIMEOUT
            );
        }
    }
    
    flush() {
        if (!this.state.pendingStorageWrites.size) return;
        if (!this._isContextValid()) {
            console.warn('[StorageManager] Extension context invalidated, skipping flush');
            this.state.pendingStorageWrites.clear();
            return;
        }
        
        try {
            let ids = [...this.state.processedVideoIds];
            if (ids.length > CONFIG.storage.MAX_HISTORY) {
                ids = ids.slice(-CONFIG.storage.MAX_HISTORY);
                this.state.processedVideoIds.clear();
                ids.forEach(id => this.state.processedVideoIds.add(id));
            }
            
            chrome.storage.local.set({ processedVideoIds: ids });
            this.state.pendingStorageWrites.clear();
            
            if (this.state.storageBatchTimer) {
                clearTimeout(this.state.storageBatchTimer);
                this.state.storageBatchTimer = null;
            }
        } catch (e) {
            console.warn('[StorageManager] Storage flush failed:', e.message);
            this.state.pendingStorageWrites.clear();
        }
    }
}

// ============================================================================
// MESSENGER
// ============================================================================

class Messenger {
    constructor(state) {
        this.state = state;
    }
    
    _isContextValid() {
        try {
            return chrome.runtime && !!chrome.runtime.id;
        } catch (e) {
            return false;
        }
    }
    
    send(type, payload) {
        if (!this.state.isActive) return;
        if (!this._isContextValid()) return;
        
        try {
            chrome.runtime.sendMessage({
                type,
                payload: {
                    scraped_by: this.state.cachedUsername,
                    session_id: this.state.sessionId,
                    timestamp: new Date().toISOString(),
                    ...payload,
                }
            }, () => chrome.runtime.lastError);
        } catch (e) {
            // Silent fail - extension context likely invalidated
        }
    }
    
    sendVideo(data) {
        this.send('NEW_DATA', {
            author_handle: data.authorHandle,
            is_verified: data.isVerified,
            is_ad: data.isAd,
            is_live: data.isLive,
            is_ai: data.isAI,
            description: data.description,
            hashtags: data.hashtags,
            mentions: data.mentions,
            place: data.place,
            place_id: data.placeId,
            stat_likes: data.statLikes,
            stat_likes_raw: data.statLikesRaw,
            stat_comments: data.statComments,
            stat_comments_raw: data.statCommentsRaw,
            stat_saved: data.statSaved,
            stat_saved_raw: data.statSavedRaw,
            stat_shared: data.statShared,
            stat_shared_raw: data.statSharedRaw,
            music_name: data.musicName,
            music_id: data.musicId,
            music_url: data.musicUrl,
            effect_name: data.effectName,
            effect_id: data.effectId,
            effect_url: data.effectUrl,
            video_id: data.platformId,
            video_url: data.videoUrl,
            session_sequence: data.sessionSequence,
            context_type: data.contextType,
        });
    }
    
    sendComments(comments) {
        if (comments.length === 0) return;
        this.send('NEW_COMMENTS', { comments });
    }
    
    sendInteractions(interactions) {
        if (interactions.length === 0) return;
        this.send('NEW_INTERACTIONS', { interactions });
    }
    
    sendSearch(searchEvent) {
        this.send('SEARCH_EVENT', searchEvent);
    }
    
    sendProfileVisit(profileData) {
        this.send('PROFILE_VISIT', profileData);
    }
    
    sendWatchTime(watchData) {
        console.log('[Messenger] sendWatchTime called with:', watchData);
        this.send('WATCH_TIME', watchData);
    }
}
