// popup.js - Extension Popup Controller

document.addEventListener('DOMContentLoaded', () => {
    // 1. SELECTORS - strictly matching your popup.html IDs
    const startBtn = document.getElementById('startBtn');
    const stopBtn = document.getElementById('stopBtn');
    const statusDot = document.getElementById('statusDot');
    const statusText = document.getElementById('statusText');
    const sendToBackendCheckbox = document.getElementById('sendToBackend');
    const saveLocallyCheckbox = document.getElementById('saveLocally');
    const downloadAllBtn = document.getElementById('downloadAllBtn');
    const videoCount = document.getElementById('videoCount');
    const commentCount = document.getElementById('commentCount');
    const profileCount = document.getElementById('profileCount');
    const searchCount = document.getElementById('searchCount');
    const interactionCount = document.getElementById('interactionCount');
    const watchTimeCount = document.getElementById('watchTimeCount');
    const usernameTester = document.getElementById('usernameTester');
    const saveUsernameBtn = document.getElementById('saveUsernameBtn');
    const currentUser = document.getElementById('currentUser');
    const displayUsername = document.getElementById('displayUsername');
    const changeUserBtn = document.getElementById('changeUserBtn');
    const resetBtn = document.getElementById('resetBtn');
    const backendUrlInput = document.getElementById('backendUrl');
    const saveBackendBtn = document.getElementById('saveBackendBtn');
    const currentBackend = document.getElementById('currentBackend');
    const displayBackend = document.getElementById('displayBackend');
    const changeBackendBtn = document.getElementById('changeBackendBtn');
    const toastContainer = document.getElementById('toastContainer');

    // Track previous counts for animation
    let prevCounts = { videos: 0, comments: 0, profiles: 0, searches: 0, interactions: 0, watchTime: 0 };

    // 2. TOAST NOTIFICATION SYSTEM
    function showToast(message, type = 'info', duration = 3000) {
        const icons = {
            success: '✓',
            error: '✕',
            warning: '⚠',
            info: 'ℹ'
        };
        
        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        toast.innerHTML = `<span class="toast-icon">${icons[type]}</span>${message}`;
        
        toastContainer.appendChild(toast);
        
        setTimeout(() => {
            toast.remove();
        }, duration);
    }

    // 3. UI UPDATE HELPERS
    function updateBackendUI(url) {
        if (url) {
            backendUrlInput.style.display = 'none';
            saveBackendBtn.style.display = 'none';
            currentBackend.style.display = 'flex';
            displayBackend.textContent = url;
        } else {
            backendUrlInput.style.display = 'block';
            saveBackendBtn.style.display = 'block';
            currentBackend.style.display = 'none';
            displayBackend.textContent = '';
        }
    }
    function updateUsernameUI(username) {
        if (username) {
            usernameTester.style.display = 'none';
            saveUsernameBtn.style.display = 'none';
            currentUser.style.display = 'flex';
            displayUsername.textContent = username;
            startBtn.disabled = false;
        } else {
            usernameTester.style.display = 'block';
            saveUsernameBtn.style.display = 'block';
            currentUser.style.display = 'none';
            displayUsername.textContent = '';
            startBtn.disabled = true;
        }
    }

    function updateUI(isActive) {
        if (isActive) {
            statusDot.classList.add('active'); // CSS should handle green color
            statusDot.style.backgroundColor = '#10b981'; // Fallback inline style
            statusText.textContent = 'Running';
            statusText.style.color = '#10b981';
            
            startBtn.disabled = true;
            stopBtn.disabled = false;
        } else {
            statusDot.classList.remove('active');
            statusDot.style.backgroundColor = '#ef4444'; // Fallback inline style
            statusText.textContent = 'Inactive';
            statusText.style.color = '#ef4444';

            startBtn.disabled = false;
            stopBtn.disabled = true;
        }
    }

    function updateAllCounts() {
        chrome.storage.local.get(['videoData', 'commentData', 'profileVisitData', 'searchData', 'interactionData', 'watchTimeData'], (result) => {
            const videos = result.videoData ? result.videoData.length : 0;
            const comments = result.commentData ? result.commentData.length : 0;
            const profiles = result.profileVisitData ? result.profileVisitData.length : 0;
            const searches = result.searchData ? result.searchData.length : 0;
            const interactions = result.interactionData ? result.interactionData.length : 0;
            const watchTime = result.watchTimeData ? result.watchTimeData.length : 0;
            
            // Update counts with animation if changed
            updateStatWithAnimation('videos', videoCount, videos, prevCounts.videos);
            updateStatWithAnimation('comments', commentCount, comments, prevCounts.comments);
            updateStatWithAnimation('profiles', profileCount, profiles, prevCounts.profiles);
            updateStatWithAnimation('searches', searchCount, searches, prevCounts.searches);
            updateStatWithAnimation('interactions', interactionCount, interactions, prevCounts.interactions);
            updateStatWithAnimation('watchtime', watchTimeCount, watchTime, prevCounts.watchTime);
            
            // Store current counts
            prevCounts = { videos, comments, profiles, searches, interactions, watchTime };
            
            // Show download button if there is any data
            const hasData = videos > 0 || comments > 0 || profiles > 0 || searches > 0 || interactions > 0 || watchTime > 0;
            if (downloadAllBtn) {
                downloadAllBtn.style.display = hasData ? 'flex' : 'none';
            }
        });
    }

    function updateStatWithAnimation(type, element, newValue, oldValue) {
        if (!element) return;
        element.textContent = newValue;
        
        if (newValue > oldValue) {
            const statItem = element.closest('.stat-item');
            if (statItem) {
                statItem.classList.remove('updated');
                void statItem.offsetWidth; // Force reflow
                statItem.classList.add('updated');
                
                // Remove class after animation
                setTimeout(() => statItem.classList.remove('updated'), 500);
            }
        }
    }

    // 3. CORE LOGIC
    async function getActiveTab() {
        const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
        return tab;
    }

    async function sendMessage(action) {
        const btn = action === 'START' ? startBtn : stopBtn;
        
        // Validate username before starting
        if (action === 'START') {
            const result = await chrome.storage.local.get(['usernameTester']);
            if (!result.usernameTester) {
                showToast('Set username first!', 'warning');
                return;
            }
        }

        const tab = await getActiveTab();
        
        // Safety check: ensure we are on TikTok
        if (!tab || !tab.url || !tab.url.includes('tiktok.com')) {
            showToast('Open TikTok first!', 'warning');
            return;
        }

        // Show loading state
        btn.classList.add('loading');

        try {
            // Attempt to send message to Content Script
            await chrome.tabs.sendMessage(tab.id, { action });
            
            // If successful, update UI and Storage
            const isActive = action === 'START';
            await chrome.storage.local.set({ scraperActive: isActive });
            updateUI(isActive);
            
            showToast(isActive ? 'Scraper started!' : 'Scraper stopped', 'success');
            
        } catch (error) {
            if (action === 'START') {
                // Content script not loaded - set scraperActive BEFORE reload
                // so it auto-starts after the page loads
                showToast('Injecting script, please wait...', 'info');
                await chrome.storage.local.set({ scraperActive: true });
                await chrome.tabs.reload(tab.id);
                updateUI(true);
            } else {
                // For STOP, just reset state
                await chrome.storage.local.set({ scraperActive: false });
                updateUI(false);
            }
        } finally {
            btn.classList.remove('loading');
        }
    }

    // 4. EVENT LISTENERS
    saveUsernameBtn.addEventListener('click', () => {
        const username = usernameTester.value.trim();
        if (username) {
            saveUsernameBtn.classList.add('loading');
            chrome.storage.local.set({ usernameTester: username }, () => {
                updateUsernameUI(username);
                saveUsernameBtn.classList.remove('loading');
                currentUser.classList.add('save-success');
                showToast(`Logged in as ${username}`, 'success');
                setTimeout(() => currentUser.classList.remove('save-success'), 500);
            });
        } else {
            showToast('Please enter a username', 'warning');
        }
    });

    changeUserBtn.addEventListener('click', () => {
        usernameTester.value = '';
        chrome.storage.local.remove('usernameTester', () => {
            updateUsernameUI(null);
        });
    });

    // Backend URL handlers
    saveBackendBtn.addEventListener('click', () => {
        const url = backendUrlInput.value.trim();
        if (url) {
            saveBackendBtn.classList.add('loading');
            chrome.storage.local.set({ backendUrl: url }, () => {
                updateBackendUI(url);
                saveBackendBtn.classList.remove('loading');
                showToast('Backend URL saved!', 'success');
            });
        } else {
            showToast('Please enter a backend URL', 'warning');
        }
    });

    changeBackendBtn.addEventListener('click', () => {
        chrome.storage.local.get(['backendUrl'], (result) => {
            backendUrlInput.value = result.backendUrl || '';
            updateBackendUI(null);
        });
    });

    backendUrlInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') saveBackendBtn.click();
    });

    usernameTester.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            saveUsernameBtn.click();
        }
    });

    startBtn.addEventListener('click', () => sendMessage('START'));
    stopBtn.addEventListener('click', () => sendMessage('STOP'));

    // Settings persistence with feedback
    sendToBackendCheckbox.addEventListener('change', () => {
        chrome.storage.local.set({ sendToBackend: sendToBackendCheckbox.checked });
        showToast(sendToBackendCheckbox.checked ? 'Backend sync enabled' : 'Backend sync disabled', 'info');
    });
    
    saveLocallyCheckbox.addEventListener('change', () => {
        chrome.storage.local.set({ saveLocally: saveLocallyCheckbox.checked });
        showToast(saveLocallyCheckbox.checked ? 'Local storage enabled' : 'Local storage disabled', 'info');
    });

    // JSON Download Logic
    downloadAllBtn.addEventListener('click', () => {
        downloadAllBtn.classList.add('loading');
        
        chrome.storage.local.get(['videoData', 'commentData', 'profileVisitData', 'searchData', 'interactionData', 'watchTimeData'], (result) => {
            const exportData = {
                exportDate: new Date().toISOString(),
                videos: result.videoData || [],
                comments: result.commentData || [],
                profiles: result.profileVisitData || [],
                searches: result.searchData || [],
                interactions: result.interactionData || [],
                watchTime: result.watchTimeData || []
            };
            
            const totalItems = exportData.videos.length + exportData.comments.length + 
                              exportData.profiles.length + exportData.searches.length +
                              exportData.interactions.length + exportData.watchTime.length;
            
            downloadAllBtn.classList.remove('loading');
            
            if (totalItems === 0) {
                showToast('No data to download', 'warning');
                return;
            }

            const jsonContent = JSON.stringify(exportData, null, 2);
            const url = URL.createObjectURL(new Blob([jsonContent], { type: 'application/json' }));
            
            chrome.downloads.download({
                url: url,
                filename: `tiktok_data_${new Date().toISOString().slice(0,10)}.json`,
                saveAs: true
            }, () => {
                showToast(`Exported ${totalItems} items`, 'success');
            });
        });
    });

    // Reset button logic
    resetBtn.addEventListener('click', () => {
        if (confirm('Are you sure you want to reset all data? This will clear:\n\n• All scraped videos\n• All comments\n• All profile visits\n• All searches\n• All interactions\n• All watch time data\n\nThis action cannot be undone!')) {
            resetBtn.classList.add('loading');
            
            // Clear all storage data
            chrome.storage.local.remove([
                'videoData', 
                'commentData', 
                'profileVisitData', 
                'searchData', 
                'interactionData',
                'watchTimeData',
                'processedVideoIds', 
                'scraperActive'
            ], () => {
                // Reset previous counts
                prevCounts = { videos: 0, comments: 0, profiles: 0, searches: 0, interactions: 0, watchTime: 0 };
                
                // Update UI
                updateAllCounts();
                updateUI(false);
                resetBtn.classList.remove('loading');
                
                showToast('All data cleared!', 'success');
            });
        }
    });

    // 5. INITIALIZATION
    // Restore state when popup opens
    chrome.storage.local.get(['scraperActive', 'sendToBackend', 'saveLocally', 'usernameTester', 'backendUrl'], (result) => {
        updateUsernameUI(result.usernameTester || null);
        updateBackendUI(result.backendUrl || null);
        updateUI(result.scraperActive || false);
        
        // If undefined, default to true
        sendToBackendCheckbox.checked = result.sendToBackend !== false; 
        saveLocallyCheckbox.checked = result.saveLocally !== false;
    });

    updateAllCounts();
    
    // Auto-refresh stats while popup is open
    setInterval(updateAllCounts, 2000);
});