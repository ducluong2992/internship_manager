/* ================================================
   page_documents.js — Quản lý Tài liệu Tri thức
   với RAG Index Status, Chunk Count, Re-index
   ================================================ */

// Map theo dõi các polling interval theo docId
const _indexingPollers = {};

async function renderDocuments(area) {
  // Dừng tất cả poller cũ khi re-render
  Object.keys(_indexingPollers).forEach(id => {
    clearInterval(_indexingPollers[id]);
    delete _indexingPollers[id];
  });

  let docs = [];
  try {
    docs = await api('GET', '/api/documents');
  } catch (e) {
    area.innerHTML = `<div class="alert alert-danger">${e.message}</div>`;
    return;
  }

  area.innerHTML = `
<div class="glass-card p-4">
  <div class="section-header mb-4">
    <div class="section-title">
      <i class="bi bi-file-earmark-text-fill text-danger"></i> Quản lý Tài liệu Tri thức
    </div>
    <button class="btn btn-outline-danger btn-sm" data-bs-toggle="modal" data-bs-target="#modal-upload">
      <i class="bi bi-cloud-upload me-1"></i>Upload tài liệu
    </button>
  </div>

  <!-- RAG Info Banner -->
  <div class="rag-info-banner mb-3">
    <i class="bi bi-cpu-fill me-2 text-primary"></i>
    <span>Tài liệu được tự động chunk và embed để phục vụ <strong>RAG semantic search</strong>.
    Chỉ những tài liệu có trạng thái <span class="badge-index-status indexed">INDEXED</span> mới được dùng trong chatbot.</span>
  </div>

  <div class="table-responsive">
    <table class="table table-hover align-middle">
      <thead>
        <tr>
          <th>Tên file</th>
          <th>Người tải lên</th>
          <th>Ngày tạo</th>
          <th>File Status</th>
          <th>Index RAG</th>
          <th>Hoạt động</th>
          <th class="text-end">Thao tác</th>
        </tr>
      </thead>
      <tbody id="doc-tbody">
        ${docs.length === 0
          ? '<tr><td colspan="7" class="text-center text-muted py-5"><i class="bi bi-inbox display-6 d-block mb-2"></i>Chưa có tài liệu nào</td></tr>'
          : docs.map(d => buildDocRow(d)).join('')}
      </tbody>
    </table>
  </div>

  <!-- Stats footer -->
  ${docs.length > 0 ? `
  <div class="doc-stats mt-3 pt-3 border-top border-light d-flex gap-4 flex-wrap">
    <span><i class="bi bi-files me-1 text-muted"></i><strong>${docs.length}</strong> tài liệu</span>
    <span><i class="bi bi-check-circle me-1 text-success"></i><strong>${docs.filter(d => d.index_status === 'INDEXED').length}</strong> đã index</span>
    <span><i class="bi bi-hourglass me-1 text-warning"></i><strong>${docs.filter(d => d.index_status === 'PENDING' || d.index_status === 'INDEXING').length}</strong> đang xử lý</span>
    <span><i class="bi bi-layers me-1 text-primary"></i><strong>${docs.reduce((s, d) => s + (d.chunk_count || 0), 0)}</strong> chunks tổng</span>
  </div>` : ''}
</div>

<!-- Upload Modal -->
<div class="modal fade" id="modal-upload" tabindex="-1">
  <div class="modal-dialog modal-dialog-centered">
    <div class="modal-content glass-modal border-0 shadow-lg">
      <div class="modal-header border-bottom border-light">
        <h5 class="modal-title fw-bold d-flex align-items-center gap-2">
          <span class="d-inline-flex align-items-center justify-content-center rounded-circle bg-primary bg-opacity-10 text-primary" style="width:32px;height:32px;">
            <i class="bi bi-cloud-upload-fill"></i>
          </span>
          Upload Tài liệu
        </h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body px-4 py-3">
        <form id="form-upload">
          <div class="mb-3">
            <label class="form-label fw-semibold small text-uppercase text-muted">Chọn file</label>
            <input type="file" id="file-input" class="form-control rounded-3" accept=".pdf,.docx,.txt" required>
            <div class="form-text text-muted mt-1">
              <i class="bi bi-info-circle me-1"></i>Hỗ trợ: PDF, DOCX, TXT. Sau khi upload, hệ thống sẽ tự động chunk và tạo embedding.
            </div>
          </div>
          <button type="submit" class="btn btn-primary w-100 rounded-3 py-2 fw-semibold" id="btn-upload-submit">
            <span class="btn-text"><i class="bi bi-upload me-2"></i>Tải lên & Index</span>
            <span class="btn-spinner d-none">
              <span class="spinner-border spinner-border-sm me-2"></span>Đang tải lên...
            </span>
          </button>
        </form>
      </div>
    </div>
  </div>
</div>
  `;

  // ── Bind events ──
  const tbody = document.getElementById('doc-tbody');

  // Toggle active/inactive
  tbody.addEventListener('change', async e => {
    if (e.target.classList.contains('doc-toggle')) {
      const tr = e.target.closest('tr');
      const id = tr.dataset.id;
      try {
        await api('PUT', `/api/documents/${id}/toggle`);
        toast('Đã cập nhật trạng thái tài liệu');
      } catch (err) {
        toast('Lỗi cập nhật: ' + err.message, 'danger');
        e.target.checked = !e.target.checked;
      }
    }
  });

  // Delete & Re-index buttons
  tbody.addEventListener('click', async e => {
    // Xóa tài liệu
    const btnDel = e.target.closest('.btn-delete-doc');
    if (btnDel) {
      const ok = await showConfirm({
        title: 'Xóa tài liệu',
        message: 'Bạn có chắc chắn muốn xóa tài liệu này? Toàn bộ chunks và embeddings cũng sẽ bị xóa.',
        okText: 'Xóa tài liệu',
        type: 'danger'
      });
      if (!ok) return;
      const tr = btnDel.closest('tr');
      const id = tr.dataset.id;
      // Dừng poller nếu đang chạy
      if (_indexingPollers[id]) {
        clearInterval(_indexingPollers[id]);
        delete _indexingPollers[id];
      }
      try {
        await api('DELETE', `/api/documents/${id}`);
        toast('Đã xóa tài liệu và toàn bộ chunks');
        renderDocuments(area);
      } catch (err) {
        toast('Lỗi xóa tài liệu: ' + err.message, 'danger');
      }
      return;
    }

    // Re-index
    const btnReindex = e.target.closest('.btn-reindex-doc');
    if (btnReindex) {
      const tr = btnReindex.closest('tr');
      const id = parseInt(tr.dataset.id);
      btnReindex.disabled = true;
      btnReindex.innerHTML = '<span class="spinner-border spinner-border-sm"></span>';
      try {
        await api('POST', `/api/documents/${id}/reindex`);
        toast('Đã trigger re-index. Hệ thống đang xử lý...', 'info');
        startIndexPolling(id, area);
        updateIndexStatusCell(tr, { index_status: 'INDEXING', chunk_count: null });
      } catch (err) {
        toast('Lỗi re-index: ' + err.message, 'danger');
        btnReindex.disabled = false;
        btnReindex.innerHTML = '<i class="bi bi-arrow-repeat"></i>';
      }
      return;
    }
  });

  // Upload form
  document.getElementById('form-upload').addEventListener('submit', async e => {
    e.preventDefault();
    const fileInput = document.getElementById('file-input');
    if (!fileInput.files.length) return;

    const file = fileInput.files[0];
    const btn = document.getElementById('btn-upload-submit');
    btn.querySelector('.btn-text').classList.add('d-none');
    btn.querySelector('.btn-spinner').classList.remove('d-none');
    btn.disabled = true;

    try {
      const formData = new FormData();
      formData.append('file', file);

      const res = await fetch(API + '/api/documents/upload', {
        method: 'POST',
        headers: { 'Authorization': 'Bearer ' + STATE.token },
        body: formData
      });

      const data = await res.json();
      if (!res.ok) throw new Error(data.detail || 'Upload failed');

      toast('Upload thành công! Đang tạo chunks và embeddings...', 'success');
      bootstrap.Modal.getInstance(document.getElementById('modal-upload')).hide();
      await renderDocuments(area);

      // Auto-start polling cho doc mới upload
      if (data.id) {
        startIndexPolling(data.id, area);
      }

    } catch (err) {
      toast('Lỗi: ' + err.message, 'danger');
    } finally {
      btn.querySelector('.btn-text').classList.remove('d-none');
      btn.querySelector('.btn-spinner').classList.add('d-none');
      btn.disabled = false;
      fileInput.value = '';
    }
  });

  // Auto-start polling cho các doc đang INDEXING
  docs.filter(d => d.index_status === 'INDEXING' || d.index_status === 'PENDING')
      .forEach(d => startIndexPolling(d.id, area));
}

// ─── Build table row HTML ─────────────────────────────────────────────────────

function buildDocRow(d) {
  return `
  <tr data-id="${d.id}">
    <td>
      <div class="fw-semibold">${d.title || d.filename}</div>
      <small class="text-muted">${d.filename}</small>
    </td>
    <td>${d.uploaded_by || '—'}</td>
    <td class="text-nowrap">${fmtDateTime(d.created_at)}</td>
    <td>
      ${d.status === 'READY'
        ? '<span class="custom-badge badge-working">READY</span>'
        : d.status === 'PROCESSING'
          ? '<span class="custom-badge" style="background:rgba(255,193,7,.18);color:#9c5700;border:1px solid rgba(255,193,7,.35)"><i class="spinner-border spinner-border-sm me-1" style="width:10px;height:10px"></i>PROCESSING</span>'
          : '<span class="custom-badge badge-resigned">ERROR</span>'}
    </td>
    <td class="index-status-cell" data-doc-id="${d.id}">
      ${buildIndexStatusHtml(d)}
    </td>
    <td>
      <div class="form-check form-switch">
        <input class="form-check-input doc-toggle" type="checkbox" ${d.is_active ? 'checked' : ''}>
      </div>
    </td>
    <td class="text-end">
      <div class="d-flex gap-1 justify-content-end">
        <button class="btn btn-sm btn-outline-primary btn-reindex-doc"
          title="Re-index tài liệu này"
          ${d.index_status === 'INDEXING' ? 'disabled' : ''}>
          <i class="bi bi-arrow-repeat"></i>
        </button>
        <button class="btn btn-sm btn-outline-danger btn-delete-doc" title="Xóa tài liệu">
          <i class="bi bi-trash"></i>
        </button>
      </div>
    </td>
  </tr>`;
}

// ─── Index status badge HTML ──────────────────────────────────────────────────

function buildIndexStatusHtml(d) {
  const status = d.index_status || 'PENDING';
  const chunks = d.chunk_count != null ? `<small class="text-muted d-block mt-1">${d.chunk_count} chunks</small>` : '';

  switch (status) {
    case 'INDEXED':
      return `<span class="badge-index-status indexed"><i class="bi bi-check-circle-fill me-1"></i>INDEXED</span>${chunks}`;
    case 'INDEXING':
      return `<span class="badge-index-status indexing">
        <span class="spinner-border spinner-border-sm me-1" style="width:9px;height:9px;border-width:1.5px"></span>INDEXING...
      </span><small class="text-muted d-block mt-1">Đang embed chunks...</small>`;
    case 'ERROR':
      return `<span class="badge-index-status error"><i class="bi bi-x-circle-fill me-1"></i>ERROR</span>`;
    case 'PENDING':
    default:
      return `<span class="badge-index-status pending"><i class="bi bi-hourglass me-1"></i>PENDING</span>`;
  }
}

// ─── Live polling cho tài liệu đang INDEXING ─────────────────────────────────

function startIndexPolling(docId, area) {
  // Nếu đã có poller cho docId này thì dừng poller cũ
  if (_indexingPollers[docId]) {
    clearInterval(_indexingPollers[docId]);
  }

  let attempts = 0;
  const MAX_ATTEMPTS = 120; // tối đa 4 phút (2s * 120)

  _indexingPollers[docId] = setInterval(async () => {
    attempts++;
    if (attempts > MAX_ATTEMPTS) {
      clearInterval(_indexingPollers[docId]);
      delete _indexingPollers[docId];
      return;
    }

    try {
      const data = await api('GET', `/api/documents/${docId}/status`);
      const tr = document.querySelector(`tr[data-id="${docId}"]`);
      if (!tr) {
        clearInterval(_indexingPollers[docId]);
        delete _indexingPollers[docId];
        return;
      }

      // Cập nhật cell trạng thái
      updateIndexStatusCell(tr, data);

      // Nếu xong (INDEXED / ERROR) → dừng polling, re-enable reindex btn
      if (data.index_status === 'INDEXED' || data.index_status === 'ERROR') {
        clearInterval(_indexingPollers[docId]);
        delete _indexingPollers[docId];

        const reindexBtn = tr.querySelector('.btn-reindex-doc');
        if (reindexBtn) {
          reindexBtn.disabled = false;
          reindexBtn.innerHTML = '<i class="bi bi-arrow-repeat"></i>';
        }

        if (data.index_status === 'INDEXED') {
          toast(`✅ Tài liệu "${data.title}" đã index xong — ${data.chunk_count} chunks`, 'success');
          // Cập nhật stats footer
          const statsEl = document.querySelector('.doc-stats');
          if (statsEl) renderDocuments(area); // full re-render để cập nhật stats
        } else {
          toast(`❌ Lỗi index tài liệu "${data.title}"`, 'danger');
        }
      }
    } catch (_) {
      // Lỗi mạng tạm thời, bỏ qua
    }
  }, 2000);
}

function updateIndexStatusCell(tr, data) {
  const cell = tr.querySelector('.index-status-cell');
  if (cell) {
    cell.innerHTML = buildIndexStatusHtml(data);
  }
}
