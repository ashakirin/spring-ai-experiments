// Main application logic. Login flow removed: the chat screen is shown immediately.
// Authentication, when needed, is handled server-side (e.g. via Spring Security
// + an OAuth2 redirect). This file only wires the chat UI.
document.addEventListener('DOMContentLoaded', async function () {
    initializeChat();

    const chatScreen = document.getElementById('chatScreen');
    const chatForm = document.getElementById('chatForm');
    const userInput = document.getElementById('userInput');
    const themeToggle = document.getElementById('themeToggle');
    const userDisplay = document.getElementById('userDisplay');
    const uploadBtn = document.getElementById('uploadBtn');
    const fileInput = document.getElementById('fileInput');
    const clearFileBtn = document.getElementById('clearFileBtn');

    const config = await loadConfig();

    // Auto-resize textarea
    userInput.addEventListener('input', function () {
        this.style.height = 'auto';
        this.style.height = this.scrollHeight + 'px';
    });

    // Submit on Enter, newline on Shift+Enter
    userInput.addEventListener('keydown', function (e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            chatForm.requestSubmit();
        }
    });

    if (uploadBtn) {
        if (isAttachmentsEnabled(config)) {
            uploadBtn.classList.remove('hidden');
        } else {
            uploadBtn.classList.add('hidden');
        }
    }

    // Initialize model selector
    const modelSelect = document.getElementById('modelSelect');
    const models = getModels(config);
    if (modelSelect && models.length > 0) {
        modelSelect.classList.remove('hidden');
        models.forEach(m => {
            const opt = document.createElement('option');
            opt.value = m.id;
            opt.textContent = m.name;
            if (m.default) opt.selected = true;
            modelSelect.appendChild(opt);
        });
        const saved = getSelectedModel();
        if (saved && models.some(m => m.id === saved)) {
            modelSelect.value = saved;
        } else {
            const def = models.find(m => m.default) || models[0];
            setSelectedModel(def.id);
        }
        modelSelect.addEventListener('change', () => setSelectedModel(modelSelect.value));
    } else if (modelSelect) {
        modelSelect.classList.add('hidden');
    }

    // No client-side login — leave the user display blank by default. The secured
    // agent overrides this with the OAuth-authenticated username after first chat.
    if (userDisplay) {
        userDisplay.textContent = '';
    }
    chatScreen.scrollTop = 0;
    userInput.focus();

    chatForm.addEventListener('submit', async function (e) {
        e.preventDefault();
        const message = userInput.value.trim();
        if (!message) return;

        if (message.length > 10000) {
            addMessage('Message too long. Please limit to 10,000 characters.', 'ai', { isError: true });
            return;
        }

        const userMsg = addMessage(message, 'user');
        requestAnimationFrame(() => {
            const headerH = document.querySelector('.chat-header').offsetHeight;
            const msgTop = userMsg.getBoundingClientRect().top + chatScreen.scrollTop - chatScreen.getBoundingClientRect().top;
            chatScreen.scrollTo({ top: msgTop - headerH - 24, behavior: 'smooth' });
        });
        userInput.value = '';
        userInput.style.height = 'auto';

        const loadingId = showLoading();
        const lastMessage = message;

        try {
            const response = await sendMessage(message, config);
            await processStreamingResponse(response, loadingId);
        } catch (error) {
            removeLoading(loadingId);
            const retryFn = async () => {
                const container = document.getElementById('messageContainer');
                const errorMsg = container.lastElementChild;
                if (errorMsg) errorMsg.remove();

                const retryLoadingId = showLoading();
                try {
                    const retryResponse = await sendMessage(lastMessage, config);
                    await processStreamingResponse(retryResponse, retryLoadingId);
                } catch (retryError) {
                    removeLoading(retryLoadingId);
                    addMessage(`Retry failed: ${retryError.message}`, 'ai', { isError: true });
                }
            };
            addMessage(`Sorry, I encountered an error: ${error.message}`, 'ai', {
                isError: true,
                retryCallback: retryFn
            });
        }
    });

    const themeIcon = document.getElementById('themeIcon');
    themeToggle.addEventListener('click', function () {
        document.documentElement.classList.toggle('light');
        const isLight = document.documentElement.classList.contains('light');
        themeIcon.textContent = isLight ? '🌙' : '☀️';
        localStorage.setItem('theme', isLight ? 'light' : 'dark');
    });

    if (localStorage.getItem('theme') === 'dark') {
        document.documentElement.classList.remove('light');
        themeIcon.textContent = '☀️';
    } else {
        document.documentElement.classList.add('light');
        themeIcon.textContent = '🌙';
    }

    if (uploadBtn && fileInput) {
        uploadBtn.addEventListener('click', () => fileInput.click());
        fileInput.addEventListener('change', (e) => {
            if (e.target.files[0]) handleFileSelect(e.target.files[0]);
        });
    }

    if (clearFileBtn) {
        clearFileBtn.addEventListener('click', clearSelectedFile);
    }
});
