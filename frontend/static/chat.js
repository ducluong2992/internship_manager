/* =============================================
   CHAT ASSISTANT — Per-User History via localStorage
   ============================================= */

(function () {
  const toggleBtn = document.getElementById('chat-toggle-btn');
  const chatWidget = document.getElementById('chat-widget');
  const chatWindow = document.getElementById('chat-window');
  const closeBtn = document.getElementById('chat-close-btn');
  const chatForm = document.getElementById('form-chat');
  const chatInput = document.getElementById('chat-input');
  const chatBody = document.getElementById('chat-body');

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

  toggleBtn.addEventListener('click', openChat);
  closeBtn.addEventListener('click', closeChat);

  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && !chatWindow.classList.contains('d-none')) {
      closeChat();
    }
  });

  // ── Per-user storage key ──
  function storageKey() {
    return `chat_history_${STATE.userId || 'guest'}`;
  }

  // ── Persist messages ──
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

    let sourceHtml = '';
    if (msg.sources && msg.sources.length > 0) {
      sourceHtml = `<div class="chat-sources mt-2"><small class="text-muted"><i class="bi bi-file-text me-1"></i>Nguồn:</small><ul>`;
      msg.sources.forEach(s => {
        sourceHtml += `<li><small>${s.title} (Trang ${s.page || 'N/A'})</small></li>`;
      });
      sourceHtml += `</ul></div>`;
    }

    const formatted = (msg.content || '').replace(/\n/g, '<br>');
    msgDiv.innerHTML = `<div class="message-content">${formatted}${sourceHtml}</div>`;
    chatBody.appendChild(msgDiv);
    chatBody.scrollTop = chatBody.scrollHeight;
  }

  // ── Init: load history for current user ──
  window.initChat = function () {
    // Show the widget
    chatWidget.style.setProperty('display', 'block', 'important');
    toggleBtn.style.setProperty('display', 'flex', 'important');
    chatWindow.classList.add('d-none');

    // Clear previous user's messages from DOM (keep the welcome bubble)
    chatBody.innerHTML = `
      <div class="chat-message ai-message">
        <div class="message-content">Xin chào <strong>${STATE.fullName || ''}</strong>! Tôi có thể giúp gì cho bạn?</div>
      </div>`;

    // Load this user's saved messages
    const history = loadHistory();
    history.forEach(msg => renderBubble(msg));
  };

  // ── Destroy: hide widget & close window on logout ──
  window.destroyChat = function () {
    chatWidget.style.setProperty('display', 'none', 'important');
    chatWindow.classList.add('d-none');
    toggleBtn.style.setProperty('display', 'none', 'important');
  };

  // ── Send message ──
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
      <span class="spinner-grow spinner-grow-sm text-primary"></span>
      <span class="spinner-grow spinner-grow-sm text-primary ms-1"></span>
      <span class="spinner-grow spinner-grow-sm text-primary ms-1"></span>
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
        body: JSON.stringify({ message: text })
      });
      const data = await res.json();
      chatBody.removeChild(loadingDiv);

      const aiMsg = {
        type: 'ai-message',
        content: res.ok ? data.answer : 'Có lỗi xảy ra: ' + (data.detail || 'Lỗi server'),
        sources: res.ok ? data.sources : []
      };
      renderBubble(aiMsg, true);

      // Save both messages to this user's history
      const history = loadHistory();
      history.push(userMsg, aiMsg);
      // Keep last 100 messages to avoid localStorage bloat
      if (history.length > 100) history.splice(0, history.length - 100);
      saveHistory(history);

    } catch (err) {
      chatBody.removeChild(loadingDiv);
      const errMsg = { type: 'ai-message', content: 'Không thể kết nối đến máy chủ.' };
      renderBubble(errMsg, true);
    } finally {
      chatInput.disabled = false;
      chatInput.focus();
    }
  });
})();
