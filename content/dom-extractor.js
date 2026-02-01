// TikTok Scraper v6.1 - DOM Extractor
// ============================================================================

'use strict';

// ============================================================================
// DOM EXTRACTOR
// ============================================================================

class DOMExtractor {
    constructor(state) {
        this.state = state;
        this.SEL = CONFIG.selectors;
        // Regex patterns for ID extraction
        this._videoIdRegex = /\/video\/(\d{13,19})/;
        this._photoIdRegex = /\/photo\/(\d{13,19})/;
        this._longIdRegex = /(\d{13,19})/;  // TikTok IDs are 13-19 digits
        this._liveVideoIdRegex = /video_id=(?:live_)?(\d{13,19})/;
    }
    
    // === Video ID Extraction (Unified with Cascading Fallbacks) ===
    
    /**
     * Extract video platform ID with cascading fallback strategy.
     * Single source of truth for video ID extraction.
     * 
     * Priority Order:
     * 1. Cache (if enabled) - fastest
     * 2. Current page URL - most reliable (/video/ID or /photo/ID)
     * 3. Live stream URL parameter (video_id=...) - for live content
     * 4. Video links within article element
     * 5. Element IDs containing 13-19 digit numbers
     * 6. Viewport scan (visible feed item)
     * 7. Any video link on page
     * 8. Hash-based fallback (for deduplication only)
     * 
     * @param {HTMLElement|null} article - Optional article element to search within
     * @param {Object} options - Extraction options
     * @param {boolean} options.isLive - Whether this is a live stream
     * @param {boolean} options.useCache - Whether to check/update cached value (default: true)
     * @param {boolean} options.allowFallback - Whether to generate fallback ID if not found (default: false)
     * @returns {string|null} - Platform ID (13-19 digit number) or null
     */
    getVideoId(article = null, options = {}) {
        const { isLive = false, useCache = true, allowFallback = false } = options;
        
        // Strategy 1: Check cache first (fastest)
        if (useCache && this.state.cachedVideoId) {
            return this.state.cachedVideoId;
        }
        
        // Strategy 2: URL parsing (most reliable for video detail/photo pages)
        // Example: https://www.tiktok.com/@zhelayaq3/video/7572524348997750028
        const urlId = this._extractFromUrl();
        if (urlId) {
            if (useCache) this.state.cachedVideoId = urlId;
            return urlId;
        }
        
        // Strategy 3: Live stream handling (check before regular links)
        // Live links contain video_id parameter: video_id=live_7601768591683898126
        if (article) {
            const liveId = this._extractLiveId(article);
            if (liveId) {
                if (useCache) this.state.cachedVideoId = liveId;
                return liveId;
            }
        }
        
        // Strategy 4: Video link href in article
        if (article) {
            const linkId = this._extractFromLinks(article);
            if (linkId) {
                if (useCache) this.state.cachedVideoId = linkId;
                return linkId;
            }
        }
        
        // Strategy 5: Element IDs with 13-19 digit numbers in article
        if (article) {
            const elementId = this._extractFromElementIds(article);
            if (elementId) {
                if (useCache) this.state.cachedVideoId = elementId;
                return elementId;
            }
        }
        
        // Strategy 6: Viewport scan (find visible video in feed)
        const viewportId = this._extractFromViewport();
        if (viewportId) {
            if (useCache) this.state.cachedVideoId = viewportId;
            return viewportId;
        }
        
        // Strategy 7: Any video link on page
        const anyLinkId = this._extractFromAnyLink();
        if (anyLinkId) {
            if (useCache) this.state.cachedVideoId = anyLinkId;
            return anyLinkId;
        }
        
        // Strategy 8: Generate fallback ID (for deduplication only)
        if (allowFallback && article) {
            const fallbackId = this._generateFallbackId(article);
            console.log('[DOMExtractor] Using fallback ID:', fallbackId);
            return fallbackId;
        }
        
        return null;
    }
    
    // Private extraction methods
    
    /**
     * Extract ID from current page URL.
     * Handles: /video/ID, /photo/ID
     */
    _extractFromUrl() {
        const url = window.location.href;
        
        // Try /video/ID first (most common)
        const videoMatch = url.match(this._videoIdRegex);
        if (videoMatch) return videoMatch[1];
        
        // Try /photo/ID (photo carousel)
        const photoMatch = url.match(this._photoIdRegex);
        if (photoMatch) return photoMatch[1];
        
        return null;
    }
    
    /**
     * Extract ID from video links within an article element.
     */
    _extractFromLinks(article) {
        const links = article.querySelectorAll(this.SEL.VIDEO_LINK);
        for (const link of links) {
            const href = link.getAttribute('href') || '';
            const match = href.match(this._videoIdRegex);
            if (match) return match[1];
        }
        return null;
    }
    
    /**
     * Extract ID from element IDs containing 13-19 digit numbers.
     */
    _extractFromElementIds(article) {
        for (const el of article.querySelectorAll('[id]')) {
            const match = el.id.match(this._longIdRegex);
            if (match) return match[1];
        }
        return null;
    }
    
    /**
     * Scan viewport for visible feed item and extract its video ID.
     */
    _extractFromViewport() {
        const items = document.querySelectorAll(this.SEL.FEED_ITEM);
        for (const item of items) {
            const rect = item.getBoundingClientRect();
            // Check if item is in upper half of viewport (currently playing)
            if (rect.top >= 0 && rect.top < window.innerHeight * 0.5) {
                // Try video link first
                const videoLink = item.querySelector(this.SEL.VIDEO_LINK);
                const videoMatch = videoLink?.href?.match(this._videoIdRegex);
                if (videoMatch) return videoMatch[1];
                
                // Try live link for live streams in feed
                const liveId = this._extractLiveId(item);
                if (liveId) return liveId;
            }
        }
        return null;
    }
    
    /**
     * Fallback: find any video link on the page.
     */
    _extractFromAnyLink() {
        const link = document.querySelector(this.SEL.VIDEO_LINK);
        const match = link?.href?.match(this._videoIdRegex);
        return match ? match[1] : null;
    }
    
    /**
     * Extract live stream ID from live link.
     * Scans all href attributes in the article to find video_id parameter.
     */
    _extractLiveId(article) {
        // Strategy 1: Scan ALL hrefs in article for video_id parameter
        const allLinks = article.querySelectorAll('a[href*="video_id"]');
        for (const link of allLinks) {
            const href = link.getAttribute('href') || '';
            const videoIdMatch = href.match(/video_id=(?:live_)?(\d{13,19})/);
            if (videoIdMatch) {
                console.log('[DOMExtractor] Live ID found in href:', videoIdMatch[1]);
                return `live_${videoIdMatch[1]}`;
            }
        }
        
        // Strategy 2: Generate a hash-based ID using username (last resort)
        const liveLink = article.querySelector(this.SEL.LIVE_LINK) || 
                         article.querySelector('[data-e2e="video-author-avatar"][href*="/live"]');
        if (liveLink) {
            const href = liveLink.getAttribute('href') || '';
            const usernameMatch = href.match(/\/@([^/]+)\/live/);
            if (usernameMatch) {
                const username = usernameMatch[1];
                const timestamp = Date.now();
                const hashId = `live_${username}_${Utils.generateHash(`${username}:${timestamp}`)}`;
                console.log('[DOMExtractor] Live ID from username hash:', hashId);
                return hashId;
            }
        }
        
        return null;
    }
    
    /**
     * Generate a hash-based fallback ID when no platform ID can be extracted.
     * This ensures uniqueness for deduplication purposes.
     */
    _generateFallbackId(article) {
        const user = article.querySelector(this.SEL.USER_LINK)?.textContent?.trim();
        const desc = article.querySelector('[data-e2e="video-desc"]')?.textContent?.trim() || '';
        const timestamp = Date.now();
        
        // Combine available identifiers for a unique hash
        if (user && desc.length > 10) {
            return `hash_${Utils.generateHash(`${user}:${desc}:${timestamp}`)}`;
        }
        
        if (user) {
            return `hash_${Utils.generateHash(`${user}:${timestamp}`)}`;
        }
        
        // Last resort: pure timestamp-based hash
        return `hash_${Utils.generateHash(`unknown:${timestamp}:${Math.random()}`)}`;
    }
    
    // === Legacy Aliases (for backward compatibility) ===
    
    extractVideoId(article) {
        return this.getVideoId(article, { useCache: false, allowFallback: true });
    }
    
    extractPlatformId(article, isLive = false) {
        return this.getVideoId(article, { isLive, useCache: false, allowFallback: false }) || '';
    }
    
    getCurrentVideoId() {
        return this.getVideoId(null, { useCache: true, allowFallback: false });
    }
    
    // === Content Detection ===
    
    isLive(article) {
        return !!(article.querySelector(this.SEL.LIVE_LINK) || 
                  article.querySelector('[data-e2e*="live"]'));
    }
    
    isAd(article) {
        return !!article.querySelector('[data-e2e="ad-tag"], [data-e2e*="ads"]');
    }
    
    isAI(article) {
        if (article.querySelector('[data-e2e="aigc-tag"]')) return true;
        
        for (const link of article.querySelectorAll('a[href]')) {
            const href = (link.getAttribute('href') || '').toLowerCase();
            if (href.includes('ai') && href.includes('generated')) return true;
        }
        return false;
    }
    
    hasAuthor(article) {
        const text = article.querySelector(this.SEL.USER_LINK)?.textContent?.trim();
        return text && text !== 'Unknown';
    }
    
    hasValidStats(article) {
        const statButtons = [...article.querySelectorAll('button')]
            .filter(b => b.querySelector('svg') && /\d/.test(b.textContent));
        
        if (statButtons.length < 2) return false;
        
        let hasNonZero = false;
        let validCount = 0;
        
        for (const btn of statButtons) {
            const text = btn.textContent?.trim() || '';
            const numMatch = text.match(/[\d.,]+[KMB]?/i);
            if (numMatch) {
                validCount++;
                const numStr = numMatch[0].replace(/[.,]/g, '');
                if (!/^0+[KMB]?$/i.test(numMatch[0])) {
                    hasNonZero = true;
                }
            }
        }
        
        return (validCount >= 2 && hasNonZero) || validCount >= 4;
    }
    
    isDataReady(article) {
        return this.hasAuthor(article) && this.hasValidStats(article);
    }
    
    // === Data Extraction ===
    
    extractAuthor(article, isLive) {
        if (isLive) {
            const href = article.querySelector(this.SEL.LIVE_LINK)?.getAttribute('href') || '';
            return href.match(/\/@([^/]+)\/live/)?.[1] || 'Unknown';
        }
        
        const userLink = article.querySelector(this.SEL.USER_LINK);
        return {
            handle: userLink?.textContent?.trim() || 'Unknown',
            verified: userLink?.querySelector('svg') !== null,
        };
    }
    
    /**
     * Extract the video author's avatar URL from the article.
     * This is used for comparing against "liked by author" indicators on comments.
     * 
     * Strategy: Find the user profile link and get the avatar image within it.
     * In TikTok's DOM, the author avatar is typically inside an <a href="/@username"> element.
     */
    extractAuthorAvatarUrl(article) {
        if (!article) return null;
        
        // Strategy 1: Find the author's profile link and get the avatar image inside
        const userLinks = article.querySelectorAll(this.SEL.USER_LINK);
        for (const link of userLinks) {
            // Look for avatar image inside the user link
            const img = link.querySelector('img');
            if (img) {
                const src = img.getAttribute('src');
                // Verify it looks like an avatar URL (contains 'avt' in path)
                if (src && src.includes('avt')) {
                    return src;
                }
            }
        }
        
        // Strategy 2: Look for avatar container near the author area
        // TikTok often uses SpanAvatarContainer or similar classes
        const avatarContainers = article.querySelectorAll('[class*="Avatar"] img, [class*="avatar"] img');
        for (const img of avatarContainers) {
            const src = img.getAttribute('src');
            if (src && src.includes('avt')) {
                // Verify this is near a user link (not a random avatar)
                const closestLink = img.closest('a[href^="/@"]');
                if (closestLink) {
                    return src;
                }
            }
        }
        
        return null;
    }
    
    extractDescription(article, author) {
        const result = { description: '', hashtags: '', place: '', placeId: '', mentions: '' };
        const descEl = article.querySelector('[data-e2e="video-desc"]');
        
        if (descEl) {
            const mentionSet = new Set();
            descEl.querySelectorAll('a[href^="/@"]').forEach(link => {
                const match = link.getAttribute('href')?.match(/\/@([^/?]+)/);
                if (match && match[1] !== author) mentionSet.add(match[1]);
            });
            result.mentions = [...mentionSet].join(', ');
            
            const clone = descEl.cloneNode(true);
            clone.querySelectorAll('a[href^="/tag/"], a[href^="/place/"], a[href^="/@"], button, p')
                 .forEach(el => el.remove());
            result.description = clone.textContent
                ?.replace(/\s+/g, ' ')
                .trim()
                .replace(new RegExp(`^${author}\\s*`), '')
                .replace(/#\w+/g, '')
                .trim() || '';
        }
        
        const tags = [];
        article.querySelectorAll(this.SEL.TAG_LINK).forEach(link => {
            const match = link.getAttribute('href')?.match(/\/tag\/([^/?]+)/);
            if (match) tags.push(match[1]);
        });
        result.hashtags = tags.join(', ');
        
        const placeLink = article.querySelector(this.SEL.PLACE_LINK);
        if (placeLink) {
            result.place = placeLink.textContent?.trim() || '';
            const segments = (placeLink.getAttribute('href') || '').split('?')[0].split('-');
            const last = segments[segments.length - 1];
            if (/^\d+$/.test(last)) result.placeId = last;
        }
        
        return result;
    }
    
    extractStats(article, isAd, isLive) {
        const stats = {
            statLikes: null, statLikesRaw: null,
            statComments: null, statCommentsRaw: null,
            statSaved: null, statSavedRaw: null,
            statShared: null, statSharedRaw: null,
        };
        
        // For You feed: use standard selectors (not browse-*)
        const extractBySelector = (selectors) => {
            for (const selector of selectors) {
                const el = article.querySelector(selector);
                if (el) {
                    const text = el.textContent?.trim();
                    if (text && /\d/.test(text)) {
                        return { raw: text, value: Utils.parseMetricValue(text) };
                    }
                }
            }
            return null;
        };
        
        // Try explicit selectors first (For You only)
        const likeData = extractBySelector(['[data-e2e="like-count"]']);
        const commentData = extractBySelector(['[data-e2e="comment-count"]']);
        const saveData = extractBySelector(['[data-e2e="undefined-count"]']);
        const shareData = extractBySelector(['[data-e2e="share-count"]']);
        
        if (likeData) { stats.statLikes = likeData.value; stats.statLikesRaw = likeData.raw; }
        if (commentData) { stats.statComments = commentData.value; stats.statCommentsRaw = commentData.raw; }
        if (saveData) { stats.statSaved = saveData.value; stats.statSavedRaw = saveData.raw; }
        if (shareData) { stats.statShared = shareData.value; stats.statSharedRaw = shareData.raw; }
        
        // Fallback: extract from buttons with numbers (position-based)
        const needsButtonFallback = !likeData || !commentData || !shareData || isLive;
        
        if (needsButtonFallback) {
            const btnStats = this._extractStatsByButtons(article, isAd, isLive);
            
            if (!likeData && btnStats.statLikes) {
                stats.statLikes = Utils.parseMetricValue(btnStats.statLikes);
                stats.statLikesRaw = btnStats.statLikes;
            }
            if (!commentData && btnStats.statComments) {
                stats.statComments = Utils.parseMetricValue(btnStats.statComments);
                stats.statCommentsRaw = btnStats.statComments;
            }
            if (!saveData && btnStats.statSaved) {
                stats.statSaved = Utils.parseMetricValue(btnStats.statSaved);
                stats.statSavedRaw = btnStats.statSaved;
            }
            if (!shareData && btnStats.statShared) {
                stats.statShared = Utils.parseMetricValue(btnStats.statShared);
                stats.statSharedRaw = btnStats.statShared;
            }
        }
        
        return stats;
    }
    
    /**
     * Extract stats from buttons by position.
     * Button order varies by video type:
     * - Normal video: like, comment, save, share (4 buttons)
     * - Ad video: like, comment, share OR like, share (2-3 buttons)
     * - Live video: like, share (2 buttons)
     */
    _extractStatsByButtons(article, isAd, isLive) {
        const stats = { statLikes: null, statComments: null, statSaved: null, statShared: null };
        
        const getNum = (btn) => {
            if (!btn) return null;
            
            // Try <strong> element first
            const strong = btn.querySelector('strong');
            if (strong && Utils.isNumeric(strong.textContent)) return strong.textContent.trim();
            
            // Try text content with number pattern
            const fullText = btn.textContent || '';
            const matches = fullText.match(/[\d.,]+[KMB]?/gi);
            if (matches && matches.length > 0) {
                for (const match of matches) {
                    const cleaned = match.trim();
                    if (Utils.isNumeric(cleaned)) return cleaned;
                }
            }
            
            // Walk text nodes
            const walker = document.createTreeWalker(btn, NodeFilter.SHOW_TEXT);
            let node;
            while (node = walker.nextNode()) {
                const text = node.textContent?.trim() || '';
                if (Utils.isNumeric(text)) return text;
            }
            
            // For live videos, allow 0
            if (isLive) return '0';
            
            return null;
        };
        
        // Find buttons with SVG icons and numbers (exclude aside/comments)
        const btns = [...article.querySelectorAll('button')]
            .filter(b => b.querySelector('svg') && !b.closest('aside'));
        
        // For live videos: only include buttons that have numbers (don't include all buttons)
        // Live videos have: Like (with count), Share (with count, usually small)
        const buttonPool = btns.filter(b => {
            const text = b.textContent?.trim() || '';
            return /\d/.test(text);
        });
        
        const n = buttonPool.length;
        
        // Live videos: like, share (2 buttons)
        if (isLive) {
            if (n >= 1) stats.statLikes = getNum(buttonPool[0]);
            if (n >= 2) stats.statShared = getNum(buttonPool[1]);
            return stats;
        }
        
        // Ad videos with 2 buttons: like, share
        if (isAd && n === 2) {
            stats.statLikes = getNum(buttonPool[0]);
            stats.statShared = getNum(buttonPool[1]);
            return stats;
        }
        
        // Ad videos with 3 buttons: like, comment, share
        if (isAd && n === 3) {
            stats.statLikes = getNum(buttonPool[0]);
            stats.statComments = getNum(buttonPool[1]);
            stats.statShared = getNum(buttonPool[2]);
            return stats;
        }
        
        // Normal videos: like, comment, save, share (4+ buttons)
        if (n >= 1) stats.statLikes = getNum(buttonPool[0]);
        if (n >= 2) stats.statComments = getNum(buttonPool[1]);
        if (n >= 3) stats.statSaved = getNum(buttonPool[2]);
        if (n >= 4) stats.statShared = getNum(buttonPool[3]);
        
        return stats;
    }
    
    extractMusic(article) {
        if (!article) return { name: '', id: '', url: '' };
        
        const musicLink = article.querySelector(this.SEL.MUSIC_LINK);
        if (!musicLink) return { name: '', id: '', url: '' };
        
        const name = musicLink.textContent?.trim() || '';
        const href = musicLink.getAttribute('href') || '';
        const id = href.match(/-(\d+)$/)?.[1] || '';
        const url = href ? `https://www.tiktok.com${href}` : '';
        
        return { name, id, url };
    }    
    
    extractEffect(article) {
        if (!article) return { name: '', id: '', url: '' };
        
        const effectLink = article.querySelector(this.SEL.EFFECT_LINK);
        if (!effectLink) return { name: '', id: '', url: '' };
        
        const href = effectLink.getAttribute('href') || '';
        const id = href.match(/-([\d]+)$/)?.[1] || '';
        const textElement = effectLink.querySelector('p');
        const name = textElement?.textContent?.trim() || '';
        const url = href ? `https://www.tiktok.com${href}` : '';
        
        return { name, id, url };
    }
}
