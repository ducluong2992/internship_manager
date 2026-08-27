/* =============================================
   CHAT ASSISTANT — Multi-Model & API Key Test
   ============================================= */

(function () {
  const toggleBtn = document.getElementById('chat-toggle-btn');
  const chatWidget = document.getElementById('chat-widget');
  const chatWindow = document.getElementById('chat-window');
  const closeBtn = document.getElementById('chat-close-btn');
  const clearBtn = document.getElementById('chat-clear-btn');
  const chatForm = document.getElementById('form-chat');
  const chatInput = document.getElementById('chat-input');
  const chatBody = document.getElementById('chat-body');
  const modelSelect = document.getElementById('chat-model-select');
  const testKeyBtn = document.getElementById('chat-test-key-btn');
  const banner = document.getElementById('chat-banner');
  const bannerText = document.getElementById('chat-banner-text');
  const bannerClose = document.getElementById('chat-banner-close');
  const quickPrompts = document.getElementById('chat-quick-prompts');

  const MODEL_DISPLAY_NAMES = {
    'gemini-3.1-flash-lite': 'Gemini 3.1 Flash Lite',
    'gemini-3.5-flash-lite': 'Gemini 3.5 Flash Lite',
    'gemini-flash-latest': 'Gemini Flash'
  };

  // ── Open / Close Chat ──
  function openChat() {
    chatWindow.classList.remove('d-none');
    toggleBtn.style.setProperty('display', 'none', 'important');
    chatInput.focus();
  }

  function closeChat() {
    chatWindow.classList.add('d-none');
    toggleBtn.style.setProperty('display', 'flex', 'important');
  }

  if (toggleBtn) toggleBtn.addEventListener('click', openChat);
  if (closeBtn) closeBtn.addEventListener('click', closeChat);

  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && chatWindow && !chatWindow.classList.contains('d-none')) {
      closeChat();
    }
  });

  // ── Model Selection Persistence ──
  function getSelectedModel() {
    if (!modelSelect) return 'gemini-3.1-flash-lite';
    return modelSelect.value || 'gemini-3.1-flash-lite';
  }

  function initModelPreference() {
    if (!modelSelect) return;
    const savedModel = localStorage.getItem('chat_selected_model');
    if (savedModel && Array.from(modelSelect.options).some(o => o.value === savedModel)) {
      modelSelect.value = savedModel;
    }
  }

  if (modelSelect) {
    modelSelect.addEventListener('change', () => {
      const val = modelSelect.value;
      localStorage.setItem('chat_selected_model', val);
      showBanner(`Đã chuyển sang mô hình <strong>${MODEL_DISPLAY_NAMES[val] || val}</strong>`, 'info', 2500);
    });
  }

  // ── Banner Helper ──
  let bannerTimer = null;
  function showBanner(html, type = 'info', autoHideMs = 4000) {
    if (!banner || !bannerText) return;
    if (bannerTimer) clearTimeout(bannerTimer);

    banner.className = `chat-banner banner-${type}`;
    bannerText.innerHTML = html;
    banner.classList.remove('d-none');

    if (autoHideMs > 0) {
      bannerTimer = setTimeout(() => {
        banner.classList.add('d-none');
      }, autoHideMs);
    }
  }

  if (bannerClose) {
    bannerClose.addEventListener('click', () => {
      if (banner) banner.classList.add('d-none');
    });
  }

  // ── Test API Key & Model Connection ──
  if (testKeyBtn) {
    testKeyBtn.addEventListener('click', async () => {
      const selectedModel = getSelectedModel();
      const modelName = MODEL_DISPLAY_NAMES[selectedModel] || selectedModel;

      testKeyBtn.disabled = true;
      testKeyBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Testing...';

      try {
        const res = await fetch(API + '/api/chat/test-key', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + STATE.token
          },
          body: JSON.stringify({ model: selectedModel })
        });

        const data = await res.json();
        if (res.ok && data.success) {
          showBanner(`<i class="bi bi-check-circle-fill me-1"></i><strong>Key thông suốt!</strong> Đã kết nối tốt với <strong>${modelName}</strong>.`, 'success', 5000);
        } else {
          const errMsg = data.message || data.detail || 'Không thể kết nối';
          showBanner(`<i class="bi bi-exclamation-triangle-fill me-1"></i><strong>Lỗi kết nối:</strong> ${errMsg}`, 'error', 6000);
        }
      } catch (err) {
        showBanner(`<i class="bi bi-x-circle-fill me-1"></i><strong>Lỗi mạng:</strong> Không thể kết nối tới server.`, 'error', 5000);
      } finally {
        testKeyBtn.disabled = false;
        testKeyBtn.innerHTML = '<i class="bi bi-lightning-charge-fill me-1"></i>Test Key';
      }
    });
  }

  // ── Per-user storage key ──
  function storageKey() {
    return `chat_history_${STATE.userId || 'guest'}`;
  }

  function saveHistory(messages) {
    try {
      localStorage.setItem(storageKey(), JSON.stringify(messages));
    } catch (_) {}
  }

  function loadHistory() {
    try {
      const raw = localStorage.getItem(storageKey());
      return raw ? JSON.parse(raw) : [];
    } catch (_) {
      return [];
    }
  }

  // ── Render one message bubble ──
  function renderBubble(msg, animate = false) {
    const msgDiv = document.createElement('div');
    msgDiv.className = `chat-message ${msg.type}`;
    if (animate) msgDiv.style.animation = 'fadeIn 0.3s ease';

    let modelTagHtml = '';
    if (msg.type === 'ai-message' && msg.model) {
      const displayModel = MODEL_DISPLAY_NAMES[msg.model] || msg.model;
      modelTagHtml = `<div class="chat-model-tag"><i class="bi bi-stars text-danger"></i>${displayModel}</div>`;
    }

    let sourceHtml = '';
    if (msg.sources && msg.sources.length > 0) {
      sourceHtml = `<div class="chat-sources mt-2"><small class="text-muted fw-bold"><i class="bi bi-file-earmark-text me-1"></i>Nguồn tài liệu:</small><ul>`;
      msg.sources.forEach(s => {
        sourceHtml += `<li>${s.title} ${s.page ? `(Trang ${s.page})` : ''}</li>`;
      });
      sourceHtml += `</ul></div>`;
    }

    const formatted = (msg.content || '').replace(/\n/g, '<br>');
    msgDiv.innerHTML = `<div class="message-content">${modelTagHtml}<div>${formatted}</div>${sourceHtml}</div>`;
    chatBody.appendChild(msgDiv);
    chatBody.scrollTop = chatBody.scrollHeight;
  }

  // ── Quick Prompt Chips ──
  if (quickPrompts) {
    quickPrompts.addEventListener('click', (e) => {
      const chip = e.target.closest('.chat-chip');
      if (!chip) return;
      const promptText = chip.getAttribute('data-prompt');
      if (promptText) {
        chatInput.value = promptText;
        chatInput.focus();
      }
    });
  }

  // ── Clear Chat History ──
  if (clearBtn) {
    clearBtn.addEventListener('click', () => {
      if (confirm('Bạn có chắc muốn xóa toàn bộ lịch sử trò chuyện của mình?')) {
        try {
          localStorage.removeItem(storageKey());
        } catch (_) {}
        resetChatView();
        showBanner('Đã xóa sạch lịch sử hội thoại.', 'info', 3000);
      }
    });
  }

  function resetChatView() {
    const selectedModel = getSelectedModel();
    const modelDisplayName = MODEL_DISPLAY_NAMES[selectedModel] || selectedModel;
    chatBody.innerHTML = `
      <div class="chat-message ai-message">
        <div class="message-content">
          <div class="chat-model-tag"><i class="bi bi-stars text-danger"></i>${modelDisplayName}</div>
          <div>Xin chào <strong>${STATE.fullName || 'bạn'}</strong>! Tôi là trợ lý AI Viettel. Bạn có câu hỏi nào về quy chế, ca trực, OT hay tài liệu không?</div>
        </div>
      </div>`;
  }

  // ── Init: load history for current user ──
  window.initChat = function () {
    if (!chatWidget) return;
    initModelPreference();

    chatWidget.style.setProperty('display', 'block', 'important');
    if (toggleBtn) toggleBtn.style.setProperty('display', 'flex', 'important');
    if (chatWindow) chatWindow.classList.add('d-none');

    resetChatView();

    const history = loadHistory();
    history.forEach(msg => renderBubble(msg));
  };

  // ── Destroy: hide widget on logout ──
  window.destroyChat = function () {
    if (chatWidget) chatWidget.style.setProperty('display', 'none', 'important');
    if (chatWindow) chatWindow.classList.add('d-none');
    if (toggleBtn) toggleBtn.style.setProperty('display', 'none', 'important');
  };

  // ── Send message ──
  if (chatForm) {
    chatForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const text = chatInput.value.trim();
      if (!text) return;

      const currentModel = getSelectedModel();
      const userMsg = { type: 'user-message', content: text };
      renderBubble(userMsg, true);
      chatInput.value = '';
      chatInput.disabled = true;

      // Loading indicator
      const loadingDiv = document.createElement('div');
      loadingDiv.className = 'chat-message ai-message typing-indicator';
      loadingDiv.innerHTML = `<div class="message-content">
        <span class="spinner-grow spinner-grow-sm text-danger"></span>
        <span class="spinner-grow spinner-grow-sm text-danger ms-1"></span>
        <span class="spinner-grow spinner-grow-sm text-danger ms-1"></span>
        <small class="text-muted ms-2" style="font-size: 11px;">Đang suy nghĩ...</small>
      </div>`;
      chatBody.appendChild(loadingDiv);
      chatBody.scrollTop = chatBody.scrollHeight;

      try {
        const res = await fetch(API + '/api/chat', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + STATE.token
          },
          body: JSON.stringify({
            message: text,
            model: currentModel
          })
        });

        const data = await res.json();
        if (loadingDiv.parentNode) chatBody.removeChild(loadingDiv);

        const aiMsg = {
          type: 'ai-message',
          content: res.ok ? data.answer : ('Có lỗi xảy ra: ' + (data.detail || 'Lỗi server')),
          sources: res.ok ? data.sources : [],
          model: (data && data.model_used) ? data.model_used : currentModel
        };
        renderBubble(aiMsg, true);

        // Save both messages to history
        const history = loadHistory();
        history.push(userMsg, aiMsg);
        if (history.length > 100) history.splice(0, history.length - 100);
        saveHistory(history);

      } catch (err) {
        if (loadingDiv.parentNode) chatBody.removeChild(loadingDiv);
        const errMsg = {
          type: 'ai-message',
          content: 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra lại mạng hoặc API Key.'
        };
        renderBubble(errMsg, true);
      } finally {
        chatInput.disabled = false;
        chatInput.focus();
      }
    });
  }
})();

