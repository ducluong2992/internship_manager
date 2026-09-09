/* =============================================
   CHAT ASSISTANT — Viettel AI Assistant
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
  const banner = document.getElementById('chat-banner');
  const bannerText = document.getElementById('chat-banner-text');
  const bannerClose = document.getElementById('chat-banner-close');
  const quickPrompts = document.getElementById('chat-quick-prompts');

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

  // ── Markdown Parser for Chat Messages ──
  function formatMarkdown(text) {
    if (!text) return '';

    // 1. Escape HTML
    let html = text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');

    // 2. Bold & Italic (***text***)
    html = html.replace(/\*\*\*(.*?)\*\*\*/g, '<strong><em>$1</em></strong>');

    // 3. Bold (**text**)
    html = html.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
    html = html.replace(/__(.*?)__/g, '<strong>$1</strong>');

    // 4. Italic (*text*)
    html = html.replace(/\*([^*\n]+)\*/g, '<em>$1</em>');

    // 5. Code block / inline code
    html = html.replace(/`([^`]+)`/g, '<code class="bg-secondary bg-opacity-25 px-1 py-0 rounded text-danger font-monospace">$1</code>');

    // 6. Bullet lists (* item or - item)
    const lines = html.split('\n');
    let inList = false;
    const processedLines = [];

    for (let line of lines) {
      const bulletMatch = line.match(/^(\s*)[*-]\s+(.+)$/);
      if (bulletMatch) {
        if (!inList) {
          processedLines.push('<ul class="chat-markdown-list mb-2 ps-3">');
          inList = true;
        }
        processedLines.push(`<li>${bulletMatch[2]}</li>`);
      } else {
        if (inList) {
          processedLines.push('</ul>');
          inList = false;
        }
        processedLines.push(line);
      }
    }
    if (inList) {
      processedLines.push('</ul>');
    }

    html = processedLines.join('\n');

    // 7. Line breaks
    html = html.replace(/\n/g, '<br>');
    html = html.replace(/<br>(<ul|<li|<\/ul|<\/li>)/g, '$1');
    html = html.replace(/(<\/ul>|<\/li>|<ul[^>]*>)<br>/g, '$1');

    return html;
  }

  // ── Render one message bubble ──
  function renderBubble(msg, animate = false) {
    const msgDiv = document.createElement('div');
    msgDiv.className = `chat-message ${msg.type}`;
    if (animate) msgDiv.style.animation = 'fadeIn 0.3s ease';

    let modelTagHtml = '';
    if (msg.type === 'ai-message' && msg.model) {
      modelTagHtml = `<div class="chat-model-tag"><i class="bi bi-stars text-danger"></i>${msg.model}</div>`;
    }

    const formatted = formatMarkdown(msg.content || '');

    msgDiv.innerHTML = `<div class="message-content">${modelTagHtml}<div>${formatted}</div></div>`;
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
    chatBody.innerHTML = `
      <div class="chat-message ai-message">
        <div class="message-content">
          <div>Xin chào <strong>${STATE.fullName || 'bạn'}</strong>! Tôi là trợ lý AI Viettel. Bạn có câu hỏi nào về quy chế, ca trực, OT hay tài liệu không?</div>
        </div>
      </div>`;
  }

  // ── Init: load history for current user ──
  window.initChat = function () {
    if (!chatWidget) return;

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
        <small class="text-muted ms-2" style="font-size: 11px;">Đang tìm kiếm tài liệu...</small>
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
            message: text
          })
        });

        const data = await res.json();
        if (loadingDiv.parentNode) chatBody.removeChild(loadingDiv);

        const aiMsg = {
          type: 'ai-message',
          content: res.ok ? data.answer : ('Có lỗi xảy ra: ' + (data.detail || data.message || 'Lỗi server')),
          sources: res.ok ? data.sources : [],
          ragUsed: res.ok ? data.rag_used : false,
          model: (data && data.model_used) ? data.model_used : ''
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
