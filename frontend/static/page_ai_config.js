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

  area.innerHTML = `
<div class="row justify-content-center">
  <div class="col-lg-8">
    <div class="glass-card p-4">
      <div class="section-header mb-4">
        <div class="section-title"><i class="bi bi-gear-fill text-secondary"></i> Cấu hình AI Assistant</div>
      </div>
      
      <form id="form-ai-config">
        <div class="row g-3">
          <div class="col-md-6">
            <label class="form-label">Provider</label>
            <input type="text" class="form-control" id="cfg-provider" value="${config.provider || 'Google Gemini'}" readonly>
          </div>
          <div class="col-md-6">
            <label class="form-label">API Key</label>
            <input type="text" class="form-control" id="cfg-api-key" placeholder="Nhập API Key mới để cập nhật" value="${maskedKey}">
            <div class="form-text text-muted">${config.api_key ? 'Đã cấu hình API Key. Nhập để thay đổi.' : 'Chưa cấu hình API Key.'}</div>
          </div>
          <div class="col-md-6">
            <label class="form-label">Chat Model</label>
            <input type="text" class="form-control" id="cfg-chat-model" value="${config.chat_model || 'gemini-flash-latest'}">
          </div>
          <div class="col-md-6">
            <label class="form-label">Embedding Model</label>
            <input type="text" class="form-control" id="cfg-embedding-model" value="${config.embedding_model || 'gemini-embedding-001'}">
            <div class="form-text text-muted">Các model hợp lệ: gemini-embedding-001, gemini-embedding-2</div>
          </div>
          <div class="col-md-4">
            <label class="form-label">Top K</label>
            <input type="number" class="form-control" id="cfg-top-k" value="${config.top_k || 5}">
          </div>
          <div class="col-md-4">
            <label class="form-label">Chunk Size</label>
            <input type="number" class="form-control" id="cfg-chunk-size" value="${config.chunk_size || 1000}">
          </div>
          <div class="col-md-4">
            <label class="form-label">Overlap</label>
            <input type="number" class="form-control" id="cfg-overlap" value="${config.overlap || 150}">
          </div>
          <div class="col-md-4">
            <label class="form-label">Temperature</label>
            <input type="number" step="0.1" max="2" min="0" class="form-control" id="cfg-temp" value="${config.temperature !== undefined ? config.temperature : 0.2}">
          </div>
          
          <div class="col-12 text-end mt-4">
            <button type="submit" class="btn btn-danger btn-sm" id="btn-save-cfg"><i class="bi bi-check-lg me-1"></i>Lưu cấu hình</button>
          </div>
        </div>
      </form>
    </div>
  </div>
</div>
  `;

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
