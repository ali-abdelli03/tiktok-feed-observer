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
    }
    
    // === Video ID Extraction ===
    
    extractVideoId(article) {
        if (!article) return null;
        
        const videoLink = article.querySelector(this.SEL.VIDEO_LINK);
        if (videoLink) {
            const match = videoLink.getAttribute('href')?.match(/\/video\/(\d+)/);
            if (match) return `vid:${match[1]}`;
        }
        
        for (const el of article.querySelectorAll('[id]')) {
            const match = el.id.match(/(\d{13,19})/);
            if (match) return `vid:${match[1]}`;
        }
        
        const user = article.querySelector(this.SEL.USER_LINK)?.textContent?.trim();
        if (user) {
            const desc = article.querySelector('[data-e2e="video-desc"]')?.textContent?.trim() || '';
            if (desc.length > 10) {
                return `${user}::${Utils.generateHash(desc)}`;
            }
        }
        
        return `article::${Date.now()}`;
    }
    
    extractPlatformId(article, isLive) {
        if (!article) return '';
        
        if (isLive) {
            return this._extractLivePlatformId(article);
        }
        
        for (const link of article.querySelectorAll(this.SEL.VIDEO_LINK)) {
            const match = link.getAttribute('href')?.match(/\/video\/(\d+)/);
            if (match) return match[1];
        }
        
        for (const el of article.querySelectorAll('[id]')) {
            const match = el.id.match(/(\d{13,19})/);
            if (match) return match[1];
        }
        
        return '';
    }
    
_extractLivePlatformId(article) {
        const href = article.querySelector(this.SEL.LIVE_LINK)?.getAttribute('href') || '';
        
        //Extract video_id parameter from href
        try {
            const urlParams = new URLSearchParams(href.split('?')[1] || '');
            const videoIdParam = urlParams.get('video_id');
            
            if (videoIdParam && videoIdParam.startsWith('live_')) {
                return videoIdParam;
            }
        } catch (e) {
            //ignored
        }
        
        // Fallback: search any ID which looks like a live video ID in element IDs
        for (const el of article.querySelectorAll('[id]')) {
            const match = el.id.match(/(\d{13,19})/);
            if (match) return `live_${match[1]}`;
        }
        
        return '';
    }
    
    getCurrentVideoId() {
        if (this.state.cachedVideoId) return this.state.cachedVideoId;
        
        const urlMatch = window.location.href.match(/\/(video|photo)\/(\d+)/);
        if (urlMatch) return (this.state.cachedVideoId = urlMatch[2]);
        
        for (const item of document.querySelectorAll(this.SEL.FEED_ITEM)) {
            const rect = item.getBoundingClientRect();
            if (rect.top >= 0 && rect.top < window.innerHeight * 0.5) {
                const match = item.querySelector(this.SEL.VIDEO_LINK)?.href?.match(/\/video\/(\d+)/);
                if (match) return (this.state.cachedVideoId = match[1]);
            }
        }
        
        const link = document.querySelector(this.SEL.VIDEO_LINK);
        const match = link?.href?.match(/\/video\/(\d+)/);
        return match ? (this.state.cachedVideoId = match[1]) : null;
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
