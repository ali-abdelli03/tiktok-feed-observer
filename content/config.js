// TikTok Scraper v6.1 - Configuration & Utilities
// ============================================================================

'use strict';

// ============================================================================
// DEBUG MODE
// ============================================================================

const DEBUG = true;  // Set to true for development, false for production

const log = DEBUG ? console.log.bind(console) : () => {};
const warn = DEBUG ? console.warn.bind(console) : () => {};

// ============================================================================
// CONFIGURATION
// ============================================================================

const CONFIG = {
    selectors: {
        // Feed selectors (For You only)
        FEED_LIST: '[data-e2e="recommend-list-container"]',
        FEED_ITEM: '[data-e2e="recommend-list-item-container"]',
        
        // Link selectors
        VIDEO_LINK: 'a[href*="/video/"]',
        USER_LINK: 'a[href^="/@"]',
        MUSIC_LINK: 'a[href^="/music/"]',
        EFFECT_LINK: 'a[href^="/effect/"]',
        TAG_LINK: 'a[href^="/tag/"]',
        PLACE_LINK: 'a[href^="/place/"]',
        LIVE_LINK: 'a[href*="/live"]',
        
        // Metric selectors (For You feed only - not browse-*)
        METRICS: {
            like: ['[data-e2e="like-count"]'],
            comment: ['[data-e2e="comment-count"]'],
            save: ['[data-e2e="undefined-count"]'],
            share: ['[data-e2e="share-count"]'],
        },
        
        // Comment section (in aside for For You feed)
        COMMENT_CONTAINER: 'aside [data-e2e="comment-list"]',
    },
    timing: {
        PROCESS_INTERVAL: 100,
        PROCESS_DELAY: 50,
        STARTUP_DELAY: 500,
        DATA_LOAD_TIMEOUT: 5000,
        DOM_SETTLE_DELAY: 200,
        STALE_THRESHOLD: 30000,
        COMMENT_DEBOUNCE: 300,
        INTERACTION_DELAY: 150,
        CONTEXT_CHECK_INTERVAL: 500,
    },
    storage: {
        BATCH_SIZE: 10,
        BATCH_TIMEOUT: 10000,
        MAX_HISTORY: 1000,
    },
    limits: {
        MAX_CONCURRENT: 5,
    },
};

// ============================================================================
// UTILITY FUNCTIONS
// ============================================================================

const Utils = {
    isNumeric: (text) => /^[\d.,]+[KMB]?$/i.test(text?.trim()),
    
    generateHash: (str) => {
        let h = 0;
        for (let i = 0; i < str.length; i++) {
            h = ((h << 5) - h + str.charCodeAt(i)) | 0;
        }
        return Math.abs(h);
    },
    
    generateCommentId: (videoId, author, text) => {
        return `cmt:${Utils.generateHash(`${videoId}:${author}:${text.slice(0, 50)}`)}`;
    },
    
    debounce: (fn, delay) => {
        let timer = null;
        return (...args) => {
            clearTimeout(timer);
            timer = setTimeout(() => fn(...args), delay);
        };
    },

    /**
     * Extract a stable fingerprint from a TikTok avatar URL.
     * 
     * TikTok avatar URLs follow this pattern:
     * https://p16-common-sign.tiktokcdn-eu.com/tos-maliva-avt-0068/{IDENTITY_HASH}~tplv-tiktokx-cropcenter:100:100.jpeg?...
     * 
     * The identity-stable portion is the hash before ~tplv- (e.g. "9c40175ca2144171e57c1dd05fa22cf6")
     * This hash is consistent across different sizes and query parameters.
     * 
     * Returns null if the URL doesn't match expected patterns.
     */
    extractAvatarFingerprint: (url) => {
        if (!url || typeof url !== 'string') return null;
        
        try {
            // Remove query parameters first
            const pathOnly = url.split('?')[0];
            
            // Match the identity hash before ~tplv- or similar presentation suffixes
            // Pattern: /tos-*-avt-*/{HASH}~tplv-
            // The hash is typically 32 hex characters
            const match = pathOnly.match(/\/([a-f0-9]{20,})~tplv-/i);
            if (match) {
                return match[1].toLowerCase();
            }
            
            // Fallback: try to extract any 32-char hex hash from the path
            // This handles edge cases where the URL structure changes slightly
            const fallbackMatch = pathOnly.match(/\/([a-f0-9]{32})(?:[~./]|$)/i);
            if (fallbackMatch) {
                return fallbackMatch[1].toLowerCase();
            }
            
            return null;
        } catch (e) {
            return null;
        }
    },

    /**
     * Parse numeric strings like "144k", "1.2M", "5,432" to pure integers
     */
    parseMetricValue: (text) => {
        if (!text) return 0;
        const cleaned = text.trim().replace(/,/g, '').replace(/\s/g, '');
        
        const match = cleaned.match(/^([\d.]+)([KMB])?$/i);
        if (!match) return 0;
        
        let value = parseFloat(match[1]);
        const suffix = (match[2] || '').toUpperCase();
        
        switch (suffix) {
            case 'K': value *= 1000; break;
            case 'M': value *= 1000000; break;
            case 'B': value *= 1000000000; break;
        }
        
        return Math.round(value);
    },
};
