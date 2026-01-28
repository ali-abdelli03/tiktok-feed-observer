// ============================================================================
// TikTok Scraper - Background Service Worker (Modular Architecture)
// ============================================================================

'use strict';

// ============================================================================
// CONFIGURATION
// ============================================================================

const CONFIG = {
    defaultBackendUrl: 'http://localhost:8080/api/tiktok',
    storageLimits: {
        videoData: 10000,
        commentData: 5000,
        searchData: 500,
        interactionData: 5000,
        profileVisitData: 1000,
        sessionData: 100,  // Keep last 100 sessions
        watchTimeData: 5000,  // Watch time events
    },
};

// Data type definitions - single source of truth
const DATA_TYPES = {
    NEW_DATA: {
        storageKey: 'videoData',
        endpoint: '',  // Base URL
        payloadKey: null,  // Direct payload (not wrapped)
        getIdFn: (item) => `${item.author_handle}::${item.video_id || item.description}`,
    },
    NEW_COMMENTS: {
        storageKey: 'commentData',
        endpoint: '/comments',
        payloadKey: 'comments',
        getIdFn: (item) => item.id,
    },
    SEARCH_EVENT: {
        storageKey: 'searchData',
        endpoint: '/search',
        payloadKey: null,
        getIdFn: null,  // No deduplication
    },
    NEW_INTERACTIONS: {
        storageKey: 'interactionData',
        endpoint: '/interactions',
        payloadKey: 'interactions',
        getIdFn: null,  // No deduplication (each event is unique)
    },
    PROFILE_VISIT: {
        storageKey: 'profileVisitData',
        endpoint: '/profile',
        payloadKey: null,
        getIdFn: null,  // No deduplication (allow multiple visits)
    },
    SESSION_START: {
        storageKey: 'sessionData',
        endpoint: '/sessions/start',
        payloadKey: null,
        getIdFn: (item) => item.session_id,  // Deduplicate by session_id
    },
    SESSION_END: {
        storageKey: 'sessionData',
        endpoint: '/sessions/end',
        payloadKey: null,
        getIdFn: null,  // Allow multiple end events (updates)
    },
    WATCH_TIME: {
        storageKey: 'watchTimeData',
        endpoint: '/watchtime',
        payloadKey: null,
        getIdFn: null,  // Each watch event is unique
    },
};

// ============================================================================
// GENERIC QUEUE PROCESSOR
// ============================================================================

class QueueProcessor {
    constructor(storageKey, options = {}) {
        this.storageKey = storageKey;
        this.queue = [];
        this.isProcessing = false;
        this.maxItems = options.maxItems || CONFIG.storageLimits[storageKey] || 5000;
        this.getIdFn = options.getIdFn || null;
    }

    enqueue(items) {
        const itemArray = Array.isArray(items) ? items : [items];
        this.queue.push(...itemArray);
        this.process();
    }

    async process() {
        if (this.isProcessing || this.queue.length === 0) return;
        this.isProcessing = true;

        try {
            const result = await chrome.storage.local.get([this.storageKey]);
            const existingData = result[this.storageKey] || [];
            
            // Build ID set if deduplication is enabled
            const existingIds = this.getIdFn 
                ? new Set(existingData.map(this.getIdFn)) 
                : null;

            let added = 0;
            while (this.queue.length > 0) {
                const item = this.queue.shift();
                
                // Check for duplicates if getIdFn is defined
                if (existingIds) {
                    const itemId = this.getIdFn(item);
                    if (existingIds.has(itemId)) continue;
                    existingIds.add(itemId);
                }
                
                existingData.push(item);
                added++;
            }

            if (added > 0) {
                // Trim to max size
                const trimmed = existingData.slice(-this.maxItems);
                await chrome.storage.local.set({ [this.storageKey]: trimmed });
            }
        } catch (error) {
            console.error(`[QueueProcessor:${this.storageKey}]`, error);
        }

        this.isProcessing = false;

        // Process any items added while we were saving
        if (this.queue.length > 0) {
            this.process();
        }
    }
}

// ============================================================================
// BACKEND SERVICE
// ============================================================================

const BackendService = {
    async getUrl() {
        const { backendUrl } = await chrome.storage.local.get(['backendUrl']);
        return backendUrl || null;
    },

    async send(endpoint, payload) {
        try {
            const baseUrl = await this.getUrl();
            if (!baseUrl) {
                console.error('[Backend] No backend URL configured');
                return { success: false, error: 'No backend URL' };
            }

            const url = endpoint ? baseUrl.replace(/\/?$/, endpoint) : baseUrl;
            
            const controller = new AbortController();
            const timeout = setTimeout(() => controller.abort(), 10000);
            
            const response = await fetch(url, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload),
                signal: controller.signal,
            });
            clearTimeout(timeout);
            
            if (response.ok) {
                console.log('[Backend] Sent to', endpoint || '/', '- OK');
                return { success: true };
            } else {
                console.error('[Backend] HTTP error:', response.status);
                return { success: false, error: `HTTP ${response.status}` };
            }
        } catch (error) {
            const errorMsg = error.name === 'AbortError' ? 'Request timeout' : error.message;
            console.error('[Backend] Send failed:', errorMsg);
            return { success: false, error: errorMsg };
        }
    },
};

// ============================================================================
// SETTINGS SERVICE
// ============================================================================

const SettingsService = {
    async get() {
        const settings = await chrome.storage.local.get(['sendToBackend', 'saveLocally']);
        return {
            sendToBackend: settings.sendToBackend !== false,
            saveLocally: settings.saveLocally !== false,
        };
    },
};

// ============================================================================
// QUEUE INSTANCES
// ============================================================================

const queues = {};
for (const [type, config] of Object.entries(DATA_TYPES)) {
    queues[config.storageKey] = new QueueProcessor(config.storageKey, {
        maxItems: CONFIG.storageLimits[config.storageKey],
        getIdFn: config.getIdFn,
    });
}

// ============================================================================
// UNIFIED MESSAGE HANDLER
// ============================================================================

async function handleMessage(type, payload) {
    const config = DATA_TYPES[type];
    if (!config) {
        console.warn(`[Background] Unknown message type: ${type}`);
        return;
    }

    const settings = await SettingsService.get();

    // Send to backend
    if (settings.sendToBackend) {
        BackendService.send(config.endpoint, payload);
    }

    // Save locally
    if (settings.saveLocally) {
        // Extract items from payload (some types wrap data in a key)
        const items = config.payloadKey ? payload[config.payloadKey] : payload;
        
        if (items && (!Array.isArray(items) || items.length > 0)) {
            queues[config.storageKey].enqueue(items);
        }
    }
}

// ============================================================================
// CHROME EXTENSION LISTENERS
// ============================================================================

chrome.runtime.onInstalled.addListener(() => {
    chrome.storage.local.get(['backendUrl'], (result) => {
        if (!result.backendUrl) {
            chrome.storage.local.set({ backendUrl: CONFIG.defaultBackendUrl });
        }
    });
});

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
    if (DATA_TYPES[message.type]) {
        handleMessage(message.type, message.payload);
        sendResponse({ received: true });
    }
    return true;
});

