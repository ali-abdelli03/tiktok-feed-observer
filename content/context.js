// TikTok Scraper v6.1 - Context Detection
// ============================================================================

'use strict';

// ============================================================================
// CONTEXT TYPES
// ============================================================================

const ContextType = {
    FOR_YOU: 'for_you',   // Main feed: tiktok.com (infinite scroll)
    PROFILE: 'profile',   // Profile page: /@username
};

// ============================================================================
// CONTEXT DETECTOR
// ============================================================================

class ContextDetector {
    constructor(state) {
        this.state = state;
        this.currentContext = null;
        this.lastVideoCreator = null;
        this.urlHistory = [];
        this.checkInterval = null;
    }
    
    detect() {
        const pathname = window.location.pathname;
        
        const isProfilePage = /^\/@[^/]+\/?$/.test(pathname);
        const isForYouPage = pathname === '/' || pathname === '/foryou' || pathname === '/en/';
        
        if (isProfilePage) {
            return ContextType.PROFILE;
        }
        
        // Default to FOR_YOU for main feed
        return ContextType.FOR_YOU;
    }
    
    /**
     * Check if the current page is a For You feed.
     * Only For You feed items are scraped.
     */
    isForYouFeed() {
        const pathname = window.location.pathname;
        return pathname === '/' || pathname === '/foryou' || pathname === '/en/';
    }
    
    getContextInfo() {
        const context = this.detect();
        const url = window.location.href;
        
        const info = {
            type: context,
            url,
            timestamp: Date.now(),
        };
        
        const videoMatch = url.match(/\/@([^/]+)\/video\/(\d+)/);
        if (videoMatch) {
            info.creator = videoMatch[1];
            info.videoId = videoMatch[2];
        }
        
        const profileMatch = url.match(/\/@([^/]+)\/?$/);
        if (profileMatch) {
            info.profileUser = profileMatch[1];
        }
        
        return info;
    }
    
    /**
     * Check if comments are visible in the aside section (For You feed).
     * In For You, comments appear in the aside when a video is expanded.
     */
    hasVisibleComments() {
        // For You feed: comments are in the aside section
        const aside = document.querySelector('aside');
        if (aside && aside.querySelector('[data-e2e="comment-level-1"]')) {
            return true;
        }
        return false;
    }
    
    reset() {
        this.urlHistory = [];
        this.lastVideoCreator = null;
    }
    
    init() {
        this.checkInterval = setInterval(() => {
            const newContext = this.detect();
            if (newContext !== this.currentContext) {
                this.currentContext = newContext;
                this.state.currentContext = newContext;
                console.log('[ContextDetector] Context changed to:', newContext);
            }
        }, CONFIG.timing.CONTEXT_CHECK_INTERVAL);
        
        this.currentContext = this.detect();
        this.state.currentContext = this.currentContext;
        console.log('[ContextDetector] Initial context:', this.currentContext);
    }
    
    cleanup() {
        if (this.checkInterval) {
            clearInterval(this.checkInterval);
            this.checkInterval = null;
        }
    }
}
