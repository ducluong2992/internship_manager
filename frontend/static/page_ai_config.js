async function renderAIConfig(area) {
  let config = {};
  try {
    config = await api('GET', '/api/ai-config');
  } catch(e) {
    area.innerHTML = `<div class="alert alert-danger">${e.message}</div>`;
    return;
  }

  let maskedKey = '';
  if (config.api_key) {
    const keyStr = config.api_key;
    maskedKey = '•'.repeat(25) + (keyStr.length > 4 ? keyStr.slice(-4) : '');
  }

  const currentChatModel = config.chat_model || 'gemini-2.5-flash';

  area.innerHTML = `
<div class="row justify-content-center">
  <div class="col-lg-7 col-md-9">
    <div class="glass-card p-4">
      <div class="section-header mb-4 d-flex justify-content-between align-items-center">
        <div class="section-title"><i class="bi bi-robot text-danger me-2"></i>Cấu hình Trợ lý AI</div>
        <button type="button" class="btn btn-outline-danger btn-sm" id="btn-test-cfg-key">
          <i class="bi bi-lightning-charge-fill me-1"></i>Kiểm tra kết nối Key
        </button>
      </div>

      <div id="cfg-test-alert" class="d-none mb-3"></div>
      
      <form id="form-ai-config">
        <div class="row g-3">
          <div class="col-12">
            <label class="form-label fw-semibold">Nhà cung cấp (Provider)</label>
            <input type="text" class="form-control" id="cfg-provider" value="${config.provider || 'Google Gemini'}" readonly>
            <div class="form-text text-muted">Dịch vụ AI được tích hợp trong hệ thống: Google Gemini.</div>
          </div>
          
          <div class="col-12">
            <label class="form-label fw-semibold">Google Gemini API Key <span class="text-danger">*</span></label>
            <div class="input-group">
              <input type="text" class="form-control font-monospace" id="cfg-api-key" placeholder="Nhập API Key mới để cập nhật" value="${maskedKey}">
            </div>
            <div class="form-text text-muted">${config.api_key ? '<i class="bi bi-shield-check text-success me-1"></i>Đã cấu hình API Key. Nhập chuỗi mới để thay đổi.' : '<i class="bi bi-exclamation-circle text-warning me-1"></i>Chưa cấu hình API Key.'}</div>
          </div>

          <div class="col-12">
            <label class="form-label fw-semibold">Model AI <span class="text-danger">*</span></label>
            <select class="form-select" id="cfg-chat-model">
              <option value="gemini-2.5-flash" ${currentChatModel === 'gemini-2.5-flash' ? 'selected' : ''}>Gemini 2.5 Flash (Khuyến nghị - Nhanh & Chính xác)</option>
              <option value="gemini-2.5-pro" ${currentChatModel === 'gemini-2.5-pro' ? 'selected' : ''}>Gemini 2.5 Pro (Suy luận sâu & Nâng cao)</option>
              <option value="gemini-2.0-flash" ${currentChatModel === 'gemini-2.0-flash' ? 'selected' : ''}>Gemini 2.0 Flash</option>
              <option value="gemini-1.5-flash" ${currentChatModel === 'gemini-1.5-flash' ? 'selected' : ''}>Gemini 1.5 Flash</option>
              <option value="gemini-1.5-pro" ${currentChatModel === 'gemini-1.5-pro' ? 'selected' : ''}>Gemini 1.5 Pro</option>
              <option value="gemini-3.1-flash-lite" ${currentChatModel === 'gemini-3.1-flash-lite' ? 'selected' : ''}>Gemini 3.1 Flash Lite</option>
              <option value="gemini-3.5-flash-lite" ${currentChatModel === 'gemini-3.5-flash-lite' ? 'selected' : ''}>Gemini 3.5 Flash Lite</option>
            </select>
            <div class="form-text text-muted">Mô hình AI sẽ được áp dụng trực tiếp cho toàn bộ câu trả lời trong Chatbox.</div>
          </div>
          
          <div class="col-12 d-flex justify-content-end gap-2 mt-4 pt-2 border-top">
            <button type="submit" class="btn btn-danger px-4" id="btn-save-cfg"><i class="bi bi-check-lg me-1"></i>Lưu cấu hình</button>
          </div>
        </div>
      </form>
    </div>
  </div>
</div>
  `;

  // Test Key in Config Page
  document.getElementById('btn-test-cfg-key').addEventListener('click', async () => {
    const testBtn = document.getElementById('btn-test-cfg-key');
    const alertBox = document.getElementById('cfg-test-alert');
    const selectedModel = document.getElementById('cfg-chat-model').value;
    const enteredKey = document.getElementById('cfg-api-key').value.trim();

    testBtn.disabled = true;
    testBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Đang test...';
    alertBox.className = 'd-none';

    const payload = { model: selectedModel };
    if (enteredKey && enteredKey !== maskedKey) {
      payload.api_key = enteredKey;
    }

    try {
      const res = await api('POST', '/api/ai-config/test-key', payload);
      alertBox.classList.remove('d-none');
      if (res.success) {
        alertBox.className = 'alert alert-success d-flex align-items-center py-2';
        alertBox.innerHTML = `<i class="bi bi-check-circle-fill me-2 fs-5"></i> <div><strong>Thành công!</strong> ${res.message} ${res.preview ? `<br><small class="text-muted">Phản hồi: "${res.preview}"</small>` : ''}</div>`;
      } else {
        alertBox.className = 'alert alert-danger d-flex align-items-center py-2';
        alertBox.innerHTML = `<i class="bi bi-exclamation-triangle-fill me-2 fs-5"></i> <div><strong>Không thành công!</strong> ${res.message}</div>`;
      }
    } catch(err) {
      alertBox.classList.remove('d-none');
      alertBox.className = 'alert alert-danger d-flex align-items-center py-2';
      alertBox.innerHTML = `<i class="bi bi-x-circle-fill me-2 fs-5"></i> <div><strong>Lỗi:</strong> ${err.message}</div>`;
    } finally {
      testBtn.disabled = false;
      testBtn.innerHTML = '<i class="bi bi-lightning-charge-fill me-1"></i>Kiểm tra kết nối Key';
    }
  });

  document.getElementById('form-ai-config').addEventListener('submit', async e => {
    e.preventDefault();
    const btn = document.getElementById('btn-save-cfg');
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang lưu...';

    const payload = {
      provider: document.getElementById('cfg-provider').value,
      chat_model: document.getElementById('cfg-chat-model').value
    };

    const apiKey = document.getElementById('cfg-api-key').value.trim();
    if (apiKey && apiKey !== maskedKey) {
      payload.api_key = apiKey;
    }

    try {
      await api('POST', '/api/ai-config', payload);
      toast('Lưu cấu hình AI thành công!');
      renderAIConfig(area);
    } catch(err) {
      toast('Lỗi: ' + err.message, 'danger');
      btn.disabled = false;
      btn.innerHTML = '<i class="bi bi-check-lg me-2"></i>Lưu cấu hình';
    }
  });
}

