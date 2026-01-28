// TikTok Scraper v6.1 - Main Entry Point
// ============================================================================

'use strict';

// ============================================================================
// MAIN SCRAPER CLASS
// ============================================================================

class TikTokScraper {
    constructor() {
        // Core services
        this.state = new StateManager();
        this.storage = new StorageManager(this.state);
        this.messenger = new Messenger(this.state);
        this.extractor = new DOMExtractor(this.state);
        this.contextDetector = new ContextDetector(this.state);
        
        // Watch time tracker
        this.watchTimeTracker = new WatchTimeTracker(this.state, this.messenger);
        
        // Trackers (pass contextDetector for richer event data)
        this.interactionTracker = new InteractionTracker(this.state, this.messenger, this.extractor, this.contextDetector);
        this.searchTracker = new SearchTracker(this.state, this.messenger, this.contextDetector);
        this.profileVisitTracker = new ProfileVisitTracker(this.state, this.messenger, this.contextDetector);
        
        // Scrapers (For You feed only)
        this.commentScraper = new CommentScraper(this.state, this.messenger, this.extractor, this.contextDetector);
        
        // Processors
        this.videoProcessor = new VideoProcessor(
            this.state, this.storage, this.messenger, this.extractor, this.interactionTracker, this.contextDetector, this.watchTimeTracker
        );
        this.observerManager = new ObserverManager(
            this.state, this.extractor, this.videoProcessor, this.searchTracker, this.profileVisitTracker, this.watchTimeTracker
        );
        
        // Context monitoring
        this.contextMonitorInterval = null;
        this.memoryCleanupInterval = null;
        
        // Initialize
        this._setupMessageListener();
        this._setupStorageListener();
        this._autoStart();
    }
    
    start() {
        if (this.state.isActive) return;
        this.state.isActive = true;
        this.state.lastTrackedUrl = window.location.href;
        
        // Generate a new session ID for this scraping session
        const sessionId = this.state.generateSessionId();
        console.log('[TikTokScraper] New session started:', sessionId);
        
        // Notify background of session start (for potential session tracking)
        this._notifySessionStart(sessionId);
        
        const context = this.contextDetector.detect();
        console.log('[TikTokScraper] Started in context:', context);
        
        this.profileVisitTracker.init();
        this.profileVisitTracker.checkUrlChange();
        
        this._startContextMonitor();
        
        setTimeout(() => {
            if (!this.state.isActive) return;
            this.observerManager.init();
            this.commentScraper.init();
        }, CONFIG.timing.STARTUP_DELAY);
    }
    
    _notifySessionStart(sessionId) {
        try {
            chrome.runtime.sendMessage({
                type: 'SESSION_START',
                payload: {
                    session_id: sessionId,
                    scraped_by: this.state.cachedUsername,
                    started_at: this.state.sessionStartedAt,
                    initial_url: window.location.href,
                    initial_context: this.contextDetector.detect(),
                    user_agent: navigator.userAgent,
                }
            }, () => chrome.runtime.lastError);
        } catch (e) {
            // Silent fail
        }
    }
    
    _notifySessionEnd() {
        if (!this.state.sessionId) return;
        
        try {
            chrome.runtime.sendMessage({
                type: 'SESSION_END',
                payload: {
                    session_id: this.state.sessionId,
                    scraped_by: this.state.cachedUsername,
                    ended_at: new Date().toISOString(),
                    stats: this.state.getStats(),
                }
            }, () => chrome.runtime.lastError);
        } catch (e) {
            // Silent fail
        }
    }
    
    stop() {
        if (!this.state.isActive) return;
        
        // Notify background of session end
        this._notifySessionEnd();
        
        this.state.isActive = false;
        
        this._stopContextMonitor();
        
        this.observerManager.cleanup();
        this.commentScraper.cleanup();
        this.interactionTracker.cleanupAll();
        this.profileVisitTracker.cleanup();
        
        this.state.pendingProcessing.clear();
        this.state.capturedData.clear();
        this.state.articleRefs.clear();
        this.state.processingInFlight = 0;
        
        this.storage.flush();
    }
    
    _startContextMonitor() {
        if (this.contextMonitorInterval) return;
        
        let lastContext = this.contextDetector.detect();
        
        this.contextMonitorInterval = setInterval(() => {
            if (!this.state.isActive) return;
            
            const currentContext = this.contextDetector.detect();
            if (currentContext !== lastContext) {
                console.log('[TikTokScraper] Context changed:', lastContext, '->', currentContext);
                lastContext = currentContext;
                
                this.commentScraper.cleanup();
                this.commentScraper.init();
            }
        }, CONFIG.timing.CONTEXT_CHECK_INTERVAL);
        
        // Periodic memory cleanup (every 2 minutes)
        this.memoryCleanupInterval = setInterval(() => {
            if (!this.state.isActive) return;
            this.state.trimCollections();
        }, 120000);
    }
    
    _stopContextMonitor() {
        if (this.contextMonitorInterval) {
            clearInterval(this.contextMonitorInterval);
            this.contextMonitorInterval = null;
        }
        if (this.memoryCleanupInterval) {
            clearInterval(this.memoryCleanupInterval);
            this.memoryCleanupInterval = null;
        }
    }
    
    _setupMessageListener() {
        chrome.runtime.onMessage.addListener((req, sender, sendResponse) => {
            switch (req.action) {
                case 'START':
                    this.start();
                    sendResponse({ success: true });
                    break;
                case 'STOP':
                    this.stop();
                    sendResponse({ success: true });
                    break;
                case 'GET_STATS':
                    sendResponse({ 
                        success: true, 
                        stats: {
                            ...this.state.getStats(),
                            context: this.contextDetector.getContextInfo(),
                        }
                    });
                    break;
                case 'GET_CONTEXT':
                    sendResponse({
                        success: true,
                        context: this.contextDetector.getContextInfo(),
                    });
                    break;
                default:
                    sendResponse({ success: false, error: 'Unknown action' });
            }
            return true;
        });
    }
    
    _setupStorageListener() {
        chrome.storage.onChanged.addListener((changes, area) => {
            if (area === 'local' && changes.usernameTester) {
                this.state.cachedUsername = changes.usernameTester.newValue || 'Unknown';
            }
        });
    }
    
    _autoStart() {
        this.state.loadFromStorage().then(() => {
            chrome.storage.local.get(['scraperActive'], (result) => {
                if (result.scraperActive) {
                    setTimeout(() => this.start(), 500);
                }
            });
        });
    }
}

// ============================================================================
// INITIALIZE
// ============================================================================

const scraper = new TikTokScraper();
