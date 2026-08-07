async function renderDocuments(area) {
  let docs = [];
  try {
    docs = await api('GET', '/api/documents');
  } catch(e) {
    area.innerHTML = `<div class="alert alert-danger">${e.message}</div>`;
    return;
  }

  area.innerHTML = `
<div class="glass-card p-4">
  <div class="section-header mb-4">
    <div class="section-title"><i class="bi bi-file-earmark-text-fill text-danger"></i> Quản lý Tài liệu Tri thức</div>
    <button class="btn btn-outline-danger btn-sm" data-bs-toggle="modal" data-bs-target="#modal-upload">
      <i class="bi bi-cloud-upload me-1"></i>Upload tài liệu
    </button>
  </div>
  
  <div class="table-responsive">
    <table class="table table-hover align-middle">
      <thead>
        <tr>
          <th>Tên file</th>
          <th>Người tải lên</th>
          <th>Ngày tạo</th>
          <th>Trạng thái</th>
          <th>Hoạt động</th>
          <th class="text-end">Thao tác</th>
        </tr>
      </thead>
      <tbody id="doc-tbody">
        ${docs.length === 0 ? '<tr><td colspan="6" class="text-center text-muted py-4">Chưa có tài liệu nào</td></tr>' : 
          docs.map(d => `
          <tr data-id="${d.id}">
            <td><strong>${d.filename}</strong></td>
            <td>${d.uploaded_by || '—'}</td>
            <td>${fmtDateTime(d.created_at)}</td>
            <td>
              ${d.status === 'READY' ? '<span class="custom-badge badge-working">READY</span>' : 
                d.status === 'PROCESSING' ? '<span class="custom-badge" style="background:rgba(255,193,7,.18);color:#9c5700;border:1px solid rgba(255,193,7,.35)"><i class="spinner-border spinner-border-sm me-1" style="width:10px;height:10px"></i>PROCESSING</span>' : 
                '<span class="custom-badge badge-resigned">ERROR</span>'}
            </td>
            <td>
              <div class="form-check form-switch">
                <input class="form-check-input doc-toggle" type="checkbox" ${d.is_active ? 'checked' : ''}>
              </div>
            </td>
            <td class="text-end">
              <button class="btn btn-sm btn-outline-danger btn-delete-doc"><i class="bi bi-trash"></i></button>
            </td>
          </tr>
          `).join('')}
      </tbody>
    </table>
  </div>
</div>

<!-- Upload Modal -->
<div class="modal fade" id="modal-upload" tabindex="-1">
  <div class="modal-dialog">
    <div class="modal-content glass-modal">
      <div class="modal-header">
        <h5 class="modal-title">Upload Tài liệu</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body">
        <form id="form-upload">
          <div class="mb-3">
            <label class="form-label">Chọn file (PDF, DOCX, TXT)</label>
            <input type="file" id="file-input" class="form-control" accept=".pdf,.docx,.txt" required>
          </div>
          <button type="submit" class="btn btn-primary w-100" id="btn-upload-submit">
            <span class="btn-text"><i class="bi bi-upload me-2"></i>Tải lên</span>
            <span class="btn-spinner d-none"><span class="spinner-border spinner-border-sm"></span></span>
          </button>
        </form>
      </div>
    </div>
  </div>
</div>
  `;

  const tbody = document.getElementById('doc-tbody');
  tbody.addEventListener('change', async e => {
    if (e.target.classList.contains('doc-toggle')) {
      const tr = e.target.closest('tr');
      const id = tr.dataset.id;
      try {
        await api('PUT', `/api/documents/${id}/toggle`);
        toast('Đã cập nhật trạng thái tài liệu');
      } catch(err) {
        toast('Lỗi cập nhật: ' + err.message, 'danger');
        e.target.checked = !e.target.checked;
      }
    }
  });

  tbody.addEventListener('click', async e => {
    const btn = e.target.closest('.btn-delete-doc');
    if (btn) {
      const ok = await showConfirm({
        title: 'Xóa tài liệu',
        message: 'Bạn có chắc chắn muốn xóa tài liệu này? Vector trong CSDL cũng sẽ bị xóa.',
        okText: 'Xóa tài liệu',
        type: 'danger'
      });
      if (!ok) return;
      const tr = btn.closest('tr');
      const id = tr.dataset.id;
      try {
        await api('DELETE', `/api/documents/${id}`);
        toast('Đã xóa tài liệu');
        renderDocuments(area);
      } catch(err) {
        toast('Lỗi xóa tài liệu: ' + err.message, 'danger');
      }
    }
  });

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
      
      toast('Upload thành công. Hệ thống đang xử lý tài liệu.');
      bootstrap.Modal.getInstance(document.getElementById('modal-upload')).hide();
      renderDocuments(area);
    } catch(err) {
      toast('Lỗi: ' + err.message, 'danger');
    } finally {
      btn.querySelector('.btn-text').classList.remove('d-none');
      btn.querySelector('.btn-spinner').classList.add('d-none');
      btn.disabled = false;
      fileInput.value = '';
    }
  });
}
