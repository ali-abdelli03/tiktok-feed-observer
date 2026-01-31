// TikTok Scraper v6.1 - Scrapers (Comments only - For You feed)
// ============================================================================

'use strict';

// ============================================================================
// COMMENT SCRAPER (For You Feed - comments in aside section)
// ============================================================================

class CommentScraper {
    constructor(state, messenger, extractor, contextDetector) {
        this.state = state;
        this.messenger = messenger;
        this.extractor = extractor;
        this.contextDetector = contextDetector;
        
        this.observer = null;
        this.currentContainer = null;
        this.currentVideoId = null;
        this.debounceTimer = null;
        this.containerObserver = null;
    }
    
    init() {
        if (this.observer) return;
        
        // Use a throttled callback to prevent excessive processing
        let throttleTimer = null;
        const throttledCheck = () => {
            if (throttleTimer) return;
            throttleTimer = setTimeout(() => {
                throttleTimer = null;
                if (this.state.isActive) {
                    this._checkForComments();
                }
            }, 200);
        };
        
        this.observer = new MutationObserver(throttledCheck);
        
        // Prefer observing a narrower scope if aside exists
        const aside = document.querySelector('aside');
        const observeTarget = aside || document.body;
        
        this.observer.observe(observeTarget, { childList: true, subtree: true });
        
        this._checkForComments();
        console.log('[CommentScraper] Initialized, observing:', observeTarget.tagName);
    }
    
    _checkForComments() {
        const container = this._findCommentContainer();
        
        if (container && container !== this.currentContainer) {
            this._startObserving(container);
        } else if (!container && this.currentContainer) {
            this._stopObserving();
        }
    }
    
    /**
     * Find comment container - For You feed uses the aside section.
     * Priority: aside with comments > comment-list
     */
    _findCommentContainer() {
        // For You feed: comments are always in the aside section
        const asides = document.querySelectorAll('aside');
        for (const aside of asides) {
            if (aside.querySelector('[data-e2e="comment-level-1"]') || aside.querySelector('[data-e2e="comment-level-2"]')) {
                return aside;
            }
        }
        
        // Fallback: check for comment-list (rare in For You)
        const commentList = document.querySelector('[data-e2e="comment-list"]');
        if (commentList && (commentList.querySelector('[data-e2e="comment-level-1"]') || commentList.querySelector('[data-e2e="comment-level-2"]'))) {
            return commentList;
        }
        
        return null;
    }
    
    _startObserving(container) {
        this._stopObserving();
        
        this.currentContainer = container;
        this.currentVideoId = this._getCurrentVideoId();
        
        if (!this.currentVideoId) {
            console.log('[CommentScraper] No video ID found');
            return;
        }
        
        console.log('[CommentScraper] Observing comments for video:', this.currentVideoId);
        
        this._scrape();
        
        this.containerObserver = new MutationObserver(() => this._debouncedScrape());
        this.containerObserver.observe(container, { childList: true, subtree: true });
    }
    
    _stopObserving() {
        if (this.containerObserver) {
            this.containerObserver.disconnect();
            this.containerObserver = null;
        }
        this.currentContainer = null;
        this.currentVideoId = null;
        clearTimeout(this.debounceTimer);
    }
    
    _getCurrentVideoId() {
        const urlMatch = window.location.href.match(/\/video\/(\d+)/);
        if (urlMatch) return urlMatch[1];
        
        return this.extractor.getCurrentVideoId();
    }
    
    _debouncedScrape() {
        clearTimeout(this.debounceTimer);
        this.debounceTimer = setTimeout(() => {
            if (this.state.isActive && this.currentVideoId && this.currentContainer) {
                const newVideoId = this._getCurrentVideoId();
                
                // If video changed, restart observation to ensure clean state
                if (newVideoId && newVideoId !== this.currentVideoId) {
                    console.log('[CommentScraper] Video changed:', this.currentVideoId, '->', newVideoId);
                    // Don't just update the ID - restart to avoid stale comment associations
                    this._stopObserving();
                    this._checkForComments();
                    return;
                }
                
                this._scrape();
            }
        }, CONFIG.timing.COMMENT_DEBOUNCE);
    }
    
    _scrape() {
        const container = this.currentContainer;
        const videoId = this.currentVideoId;
        if (!container || !videoId) return;
        
        const readyComments = [];
        
        // Scrape both level 1 and level 2 comments
        const level1Comments = container.querySelectorAll('[data-e2e="comment-level-1"]');
        const level2Comments = container.querySelectorAll('[data-e2e="comment-level-2"]');
        
        console.log('[CommentScraper] Found', level1Comments.length, 'level-1 and', level2Comments.length, 'level-2 comments');
        
        // Process level 1 comments
        for (const commentDiv of level1Comments) {
            const commentData = this._extractComment(commentDiv, 1);
            const comment = this._processCommentData(commentData, videoId);
            if (comment) {
                readyComments.push(comment);
            }
        }
        
        // Process level 2 comments (replies)
        for (const commentDiv of level2Comments) {
            const commentData = this._extractComment(commentDiv, 2);
            const comment = this._processCommentData(commentData, videoId);
            if (comment) {
                readyComments.push(comment);
            }
        }
        
        if (readyComments.length > 0) {
            console.log('[CommentScraper] Sending', readyComments.length, 'comments');
            this.messenger.sendComments(readyComments);
        }
    }
    
    _processCommentData(commentData, videoId) {
        if (!commentData || !commentData.authorHandle) {
            return null;
        }
        
        const textForId = (commentData.textComment || '').slice(0, 50);
        const commentId = Utils.generateCommentId(videoId, commentData.authorHandle, textForId);
        
        if (this.state.processedCommentIds.has(commentId)) {
            return null;
        }
        
        // Process comment regardless of whether likes are found
        // (TikTok may change their markup for like counts)
        const comment = {
            id: commentId,
            video_id: videoId,
            author_handle: commentData.authorHandle,
            author_name: commentData.authorName,
            text_comment: commentData.textComment,
            image_url: commentData.imageUrl,
            likes: commentData.likes ?? 0,
            liked_by_author: commentData.likedByAuthor ?? false,
            mentions: commentData.mentions || [],  // Array of mentioned users
        };
        
        this.state.processedCommentIds.add(commentId);
        this.state.capturedComments.push(comment);
        
        console.log('[CommentScraper] Comment ready from @' + commentData.authorHandle);
        return comment;
    }
    
    _extractComment(commentDiv, level = 1) {
        // IMPORTANT: Always use commentDiv as the text extraction root
        // to prevent text leakage from sibling comments.
        // Only expand scope for finding author link, not for text extraction.
        const textExtractionRoot = commentDiv;
        let authorLink = null;
        
        // Find the comment author link - it's typically in the header area, NOT inside comment text
        // Strategy 1: Look for author link in comment header/username area first
        const usernameSelector = level === 2 ? '[data-e2e="comment-username-2"]' : '[data-e2e="comment-username-1"]';
        const usernameArea = textExtractionRoot.querySelector(usernameSelector);
        if (usernameArea) {
            // Look for author link within or near the username area
            authorLink = usernameArea.querySelector('a[href^="/@"]') || 
                        usernameArea.closest('*')?.querySelector('a[href^="/@"]');
        }
        
        // Strategy 2: Look for author link OUTSIDE the comment text span
        // Since textExtractionRoot IS the comment-level-X element (the text container),
        // all links inside it are mentions, NOT the author link.
        // We need to search UPWARD to find the comment wrapper, then look for the author link.
        if (!authorLink) {
            // Go up to find the comment item wrapper
            let commentWrapper = commentDiv.parentElement;
            for (let i = 0; i < 5 && commentWrapper; i++) {
                // Look for author links in this wrapper, excluding those inside our commentDiv
                const allLinks = commentWrapper.querySelectorAll('a[href^="/@"]');
                for (const link of allLinks) {
                    // Skip if this link is inside the comment text (it's a mention)
                    if (commentDiv.contains(link)) {
                        continue;
                    }
                    // Found an author link outside the comment text
                    authorLink = link;
                    break;
                }
                if (authorLink) break;
                
                // Stop at list boundaries
                if (commentWrapper.matches?.('[data-e2e="comment-list"], aside')) break;
                commentWrapper = commentWrapper.parentElement;
            }
        }
        
        // Strategy 3: Search upward if still not found (original fallback)
        if (!authorLink) {
            let current = commentDiv;
            for (let i = 0; i < 3; i++) {
                current = current.parentElement;
                if (!current) break;
                
                // Stop if we hit another comment container (prevents cross-comment contamination)
                if (current.matches?.('[data-e2e="comment-level-1"], [data-e2e="comment-level-2"]')) {
                    break;
                }
                
                // Stop if we hit the comment list container
                if (current.matches?.('[data-e2e="comment-list"], aside')) {
                    break;
                }
                
                authorLink = current.querySelector('a[href^="/@"]');
                if (authorLink) {
                    break;
                }
            }
        }
        
        if (!authorLink) {
            return null;
        }
        
        const href = authorLink.getAttribute('href') || '';
        let authorHandle = null;
        
        // Extract handle from href (e.g., "/@.marina.hfln" -> ".marina.hfln")
        const handleMatch = href.match(/\/@([^/?]+)/);
        if (handleMatch) {
            authorHandle = handleMatch[1];
        }
        
        // Fallback for encoded URLs (MS4wLjABAAAA pattern)
        if (!authorHandle && /\/@MS4wLjABAAAA/.test(href)) {
            const linkText = authorLink.textContent?.trim();
            if (linkText && linkText.startsWith('@')) {
                authorHandle = linkText.substring(1);
            } else if (linkText) {
                authorHandle = linkText;
            }
            
            if (!authorHandle) {
                const pTag = authorLink.querySelector('p');
                if (pTag) {
                    const pText = pTag.textContent?.trim();
                    if (pText && pText.startsWith('@')) {
                        authorHandle = pText.substring(1);
                    } else if (pText) {
                        authorHandle = pText;
                    }
                }
            }
        }
        
        if (!authorHandle) {
            return null;
        }
        
        let authorName = null;
        
        // Primary strategy: Get display name from <p> tag inside the author link
        const pTag = authorLink.querySelector('p');
        if (pTag) {
            authorName = pTag.textContent?.trim() || null;
        }
        
        // Fallback: Use data-e2e selectors
        if (!authorName) {
            const usernameSelector = level === 2 ? '[data-e2e="comment-username-2"]' : '[data-e2e="comment-username-1"]';
            const usernameEl = textExtractionRoot.querySelector(usernameSelector) || commentDiv.querySelector(usernameSelector);
            if (usernameEl) {
                authorName = usernameEl.textContent?.trim() || null;
            }
        }
        
        // Extract raw text from comment-level-X element
        const textComment = commentDiv.textContent?.trim() || null;
        
        // The like button and image are often OUTSIDE the comment-level-X div, in a sibling or parent wrapper.
        // Try to find the comment item wrapper that contains both the comment and like button/image.
        let commentItemRoot = commentDiv;
        
        // Look for a parent wrapper that might contain the like button or image
        // Common patterns: comment item container, list item, etc.
        let parent = commentDiv.parentElement;
        for (let i = 0; i < 3 && parent; i++) {
            // Check if this parent has an aria-pressed element (like button) or comment-thumbnail
            if (parent.querySelector('[aria-pressed]') || parent.querySelector('[data-e2e="comment-thumbnail"]')) {
                commentItemRoot = parent;
                break;
            }
            // Stop at comment list boundaries
            if (parent.matches?.('[data-e2e="comment-list"], aside')) {
                break;
            }
            parent = parent.parentElement;
        }
        
        let imageUrl = null;
        // Search for image in the expanded comment item area
        const imgEl = commentItemRoot.querySelector('[data-e2e="comment-thumbnail"]');
        if (imgEl) {
            imageUrl = imgEl.getAttribute('src') || null;
        }
        
        let likes = null;
        
        // Strategy 1: aria-pressed elements with direct span child (current TikTok pattern)
        const ariaPressedElements = commentItemRoot.querySelectorAll('[aria-pressed]');
        for (const el of ariaPressedElements) {
            const span = el.querySelector(':scope > span');
            if (span) {
                const text = span.textContent?.trim();
                if (text && /^[\d.,]+[KMB]?$/i.test(text)) {
                    likes = Utils.parseMetricValue(text);
                    break;
                }
            }
        }
        
        // Strategy 2: Find SVG (heart icon) and check its sibling span
        if (likes === null) {
            const svgs = commentItemRoot.querySelectorAll('svg');
            for (const svg of svgs) {
                // Check next sibling
                let sibling = svg.nextElementSibling;
                if (sibling && sibling.tagName === 'SPAN') {
                    const text = sibling.textContent?.trim();
                    if (text && /^[\d.,]+[KMB]?$/i.test(text)) {
                        likes = Utils.parseMetricValue(text);
                        break;
                    }
                }
            }
        }
        
        // Strategy 3: Look for like-count data-e2e attribute
        if (likes === null) {
            const likeCountEl = commentItemRoot.querySelector('[data-e2e*="like-count"], [data-e2e*="likes"]');
            if (likeCountEl) {
                const text = likeCountEl.textContent?.trim();
                if (text && /^[\d.,]+[KMB]?$/i.test(text)) {
                    likes = Utils.parseMetricValue(text);
                }
            }
        }
        
        // Detect if this comment is liked by the video author
        const likedByAuthor = this._detectAuthorLiked(commentItemRoot);
        
        // Extract mentions from comment text (optional data)
        const mentions = this._extractMentions(textExtractionRoot);
        
        return {
            authorHandle,
            authorName,
            textComment,
            imageUrl,
            likes,
            likedByAuthor,
            mentions, // Array of {displayName, encodedUrl} objects
        };
    }
    
    /**
     * Extract user mentions from comment text.
     * These are @username links within the comment content.
     * Returns array of {displayName, encodedUrl} objects.
     */
    _extractMentions(commentDiv) {
        const mentions = [];
        
        // The commentDiv IS the data-e2e="comment-level-X" element itself,
        // so we look for links directly inside it (not for nested comment-level elements)
        // All links inside the comment text span are mentions
        const mentionLinks = commentDiv.querySelectorAll('a[href^="/@"]');
        
        for (const link of mentionLinks) {
            const href = link.getAttribute('href') || '';
            const displayName = link.textContent?.trim() || '';
            
            if (displayName && href) {
                mentions.push({
                    displayName: displayName.startsWith('@') ? displayName.substring(1) : displayName,
                    encodedUrl: href,
                    isEncoded: href.includes('MS4wLjABAAAA')
                });
            }
        }
        
        return mentions;
    }
    
    /**
     * Detect if a comment is liked by the video author.
     * 
     * TikTok shows a small avatar (typically 16x16) near the like area when the author likes a comment.
     * This avatar matches the video author's profile picture.
     * 
     * Detection Strategy (ranked by confidence):
     * 1. STRONG: Avatar fingerprint matches video author's avatar fingerprint
     * 2. MEDIUM: Small avatar present with heart icon nearby (structural indicator)
     * 3. WEAK: Any small avatar in the comment sub-content area
     * 
     * Returns: { isLiked: boolean, confidence: 'strong'|'medium'|'weak'|'none' }
     * For simplicity, we return just boolean but log confidence for debugging.
     */
    _detectAuthorLiked(commentItemRoot) {
        if (!commentItemRoot) return false;
        
        // Get the video author's avatar fingerprint from state
        const authorFingerprint = this.state.cachedVideoAuthorAvatarFingerprint;
        
        // Find the "liked by author" indicator avatar
        // It's typically a small image (16x16) with alt="avatar" or in a FloaterBounds wrapper
        const likedByAuthorAvatars = this._findAuthorLikedAvatars(commentItemRoot);
        
        if (likedByAuthorAvatars.length === 0) {
            return false;
        }
        
        for (const avatarImg of likedByAuthorAvatars) {
            const avatarUrl = avatarImg.getAttribute('src');
            if (!avatarUrl) continue;
            
            // Strategy 1 (STRONG): Compare avatar fingerprints
            if (authorFingerprint) {
                const likedAvatarFingerprint = Utils.extractAvatarFingerprint(avatarUrl);
                if (likedAvatarFingerprint && likedAvatarFingerprint === authorFingerprint) {
                    console.log('[CommentScraper] Author-liked detected (STRONG): fingerprint match');
                    return true;
                }
            }
            
            // Strategy 2 (MEDIUM): Structural indicator - avatar with heart icon nearby
            const hasHeartIcon = this._hasHeartIconNearby(avatarImg);
            if (hasHeartIcon) {
                console.log('[CommentScraper] Author-liked detected (MEDIUM): avatar + heart icon');
                return true;
            }
        }
        
        // Strategy 3 (WEAK): If we found avatars but couldn't verify, don't assume liked
        // This prevents false positives when the fingerprint comparison fails
        // We only return true with STRONG or MEDIUM confidence
        return false;
    }
    
    /**
     * Find potential "liked by author" avatar images in the comment.
     * These are typically small avatars (16x16 or similar) in the comment sub-content area.
     */
    _findAuthorLikedAvatars(commentItemRoot) {
        const candidates = [];
        
        // Strategy 1: Find images with alt="avatar" (common TikTok pattern)
        const avatarAltImages = commentItemRoot.querySelectorAll('img[alt="avatar"]');
        for (const img of avatarAltImages) {
            // Verify it's a small avatar (typically 16-24px)
            const width = parseInt(img.getAttribute('width') || img.style.width, 10);
            const height = parseInt(img.getAttribute('height') || img.style.height, 10);
            if ((width && width <= 32) || (height && height <= 32)) {
                candidates.push(img);
            } else if (!width && !height) {
                // No explicit size, include but check later
                candidates.push(img);
            }
        }
        
        // Strategy 2: Find images in FloaterBounds wrapper (TikTok's author-like indicator container)
        const floaterWrappers = commentItemRoot.querySelectorAll('[class*="FloaterBounds"], [class*="floater"]');
        for (const wrapper of floaterWrappers) {
            const imgs = wrapper.querySelectorAll('img');
            for (const img of imgs) {
                if (!candidates.includes(img)) {
                    candidates.push(img);
                }
            }
        }
        
        // Strategy 3: Find small avatars near heart SVGs in the comment sub-content area
        const subContentWrappers = commentItemRoot.querySelectorAll('[class*="SubContent"], [class*="subcontent"]');
        for (const wrapper of subContentWrappers) {
            const imgs = wrapper.querySelectorAll('img');
            for (const img of imgs) {
                const src = img.getAttribute('src') || '';
                // Must be an avatar URL, not a comment image
                if (src.includes('avt') && !candidates.includes(img)) {
                    candidates.push(img);
                }
            }
        }
        
        return candidates;
    }
    
    /**
     * Check if there's a heart icon near the avatar image.
     * TikTok shows a small heart icon overlaid on or near the author's avatar.
     */
    _hasHeartIconNearby(avatarImg) {
        // Check parent container for heart SVG
        const parent = avatarImg.parentElement;
        if (!parent) return false;
        
        // Walk up a few levels looking for heart SVG
        let current = parent;
        for (let i = 0; i < 3 && current; i++) {
            // Look for SVG with heart-like path (contains path with fill="#FE2C55" or similar)
            const svgs = current.querySelectorAll('svg');
            for (const svg of svgs) {
                // Check for TikTok's heart icon color or clip-path ID
                const html = svg.innerHTML.toLowerCase();
                if (html.includes('fe2c55') || // TikTok's heart red color
                    html.includes('like') || 
                    html.includes('heart')) {
                    return true;
                }
                // Check for the specific circle heart icon (used in author-liked indicator)
                if (svg.querySelector('[fill="#FE2C55"]') || 
                    svg.querySelector('[fill="#fff"]')?.closest('svg')?.querySelector('[fill="#FE2C55"]')) {
                    return true;
                }
            }
            current = current.parentElement;
        }
        
        // Also check siblings
        const siblings = [...(parent.children || [])];
        for (const sibling of siblings) {
            if (sibling === avatarImg) continue;
            if (sibling.tagName === 'SVG' || sibling.querySelector?.('svg')) {
                const svg = sibling.tagName === 'SVG' ? sibling : sibling.querySelector('svg');
                if (svg) {
                    const html = svg.innerHTML.toLowerCase();
                    if (html.includes('fe2c55') || html.includes('like') || html.includes('heart')) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    _collectAllText(element) {
        const texts = [];
        const textElements = element.querySelectorAll('span, p');
        
        for (const el of textElements) {
            if (el.closest('button')) continue;
            
            const text = el.textContent?.trim();
            if (text && text.length > 0) {
                if (!texts.includes(text)) {
                    texts.push(text);
                }
            }
        }
        
        return texts.join(' | ');
    }
    
    cleanup() {
        if (this.observer) {
            this.observer.disconnect();
            this.observer = null;
        }
        this._stopObserving();
    }
}
