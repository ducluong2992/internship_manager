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

  const currentChatModel = config.chat_model || 'gemini-3.1-flash-lite';

  area.innerHTML = `
<div class="row justify-content-center">
  <div class="col-lg-8">
    <div class="glass-card p-4">
      <div class="section-header mb-4 d-flex justify-content-between align-items-center">
        <div class="section-title"><i class="bi bi-robot text-danger me-2"></i>Cấu hình Trợ lý AI & Gemini Models</div>
        <button type="button" class="btn btn-outline-danger btn-sm" id="btn-test-cfg-key">
          <i class="bi bi-lightning-charge-fill me-1"></i>Kiểm tra kết nối Key
        </button>
      </div>

      <div id="cfg-test-alert" class="d-none mb-3"></div>
      
      <form id="form-ai-config">
        <div class="row g-3">
          <div class="col-md-6">
            <label class="form-label fw-semibold">Provider</label>
            <input type="text" class="form-control" id="cfg-provider" value="${config.provider || 'Google Gemini'}" readonly>
          </div>
          <div class="col-md-6">
            <label class="form-label fw-semibold">API Key</label>
            <div class="input-group">
              <input type="text" class="form-control" id="cfg-api-key" placeholder="Nhập API Key mới để cập nhật" value="${maskedKey}">
            </div>
            <div class="form-text text-muted">${config.api_key ? 'Đã cấu hình API Key. Nhập chuỗi mới để thay đổi.' : 'Chưa cấu hình API Key.'}</div>
          </div>
          <div class="col-md-6">
            <label class="form-label fw-semibold">Chat Model mặc định</label>
            <select class="form-select" id="cfg-chat-model">
              <option value="gemini-3.1-flash-lite" ${currentChatModel === 'gemini-3.1-flash-lite' ? 'selected' : ''}>gemini-3.1-flash-lite (Khuyến nghị)</option>
              <option value="gemini-3.5-flash-lite" ${currentChatModel === 'gemini-3.5-flash-lite' ? 'selected' : ''}>gemini-3.5-flash-lite</option>
              <option value="gemini-flash-latest" ${currentChatModel === 'gemini-flash-latest' ? 'selected' : ''}>gemini-flash-latest</option>
              <option value="gemini-flash-lite-latest" ${currentChatModel === 'gemini-flash-lite-latest' ? 'selected' : ''}>gemini-flash-lite-latest</option>
              <option value="gemini-2.5-flash-lite" ${currentChatModel === 'gemini-2.5-flash-lite' ? 'selected' : ''}>gemini-2.5-flash-lite</option>
            </select>
          </div>
          <div class="col-md-6">
            <label class="form-label fw-semibold">Embedding Model</label>
            <input type="text" class="form-control" id="cfg-embedding-model" value="${config.embedding_model || 'gemini-embedding-001'}">
            <div class="form-text text-muted">Model tạo vector tài liệu: gemini-embedding-001, text-embedding-004</div>
          </div>
          <div class="col-md-3">
            <label class="form-label">Top K (Số đoạn trích)</label>
            <input type="number" class="form-control" id="cfg-top-k" value="${config.top_k || 5}">
          </div>
          <div class="col-md-3">
            <label class="form-label">Chunk Size</label>
            <input type="number" class="form-control" id="cfg-chunk-size" value="${config.chunk_size || 1000}">
          </div>
          <div class="col-md-3">
            <label class="form-label">Overlap</label>
            <input type="number" class="form-control" id="cfg-overlap" value="${config.overlap || 150}">
          </div>
          <div class="col-md-3">
            <label class="form-label">Temperature</label>
            <input type="number" step="0.1" max="2" min="0" class="form-control" id="cfg-temp" value="${config.temperature !== undefined ? config.temperature : 0.2}">
          </div>
          
          <div class="col-12 d-flex justify-content-end gap-2 mt-4">
            <button type="submit" class="btn btn-danger btn-sm px-4" id="btn-save-cfg"><i class="bi bi-check-lg me-1"></i>Lưu cấu hình</button>
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
      chat_model: document.getElementById('cfg-chat-model').value,
      embedding_model: document.getElementById('cfg-embedding-model').value,
      top_k: parseInt(document.getElementById('cfg-top-k').value),
      chunk_size: parseInt(document.getElementById('cfg-chunk-size').value),
      overlap: parseInt(document.getElementById('cfg-overlap').value),
      temperature: parseFloat(document.getElementById('cfg-temp').value)
    };

    const apiKey = document.getElementById('cfg-api-key').value.trim();
    if (apiKey && apiKey !== maskedKey) {
      payload.api_key = apiKey;
    }

    try {
      await api('POST', '/api/ai-config', payload);
      toast('Lưu cấu hình thành công!');
      renderAIConfig(area);
    } catch(err) {
      toast('Lỗi: ' + err.message, 'danger');
      btn.disabled = false;
      btn.innerHTML = '<i class="bi bi-check-lg me-2"></i>Lưu cấu hình';
    }
  });
}

