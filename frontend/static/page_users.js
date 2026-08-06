// ═══════════════════════════════════════════
// MANAGE USERS (Admin)
// ═══════════════════════════════════════════
async function renderManageUsers(area) {
  let users = await api('GET', '/admin/users');
  let filter = '';
  let filterStatus = '';

  function filtered() {
    return users.filter(u => u.user_type === 'intern' &&
      (!filterStatus || u.working_status === filterStatus) &&
      (u.full_name.toLowerCase().includes(filter) ||
        u.employee_code.toLowerCase().includes(filter) ||
        (u.project || '').toLowerCase().includes(filter)));
  }

  function render() {
    const searchEl = document.getElementById('user-search');
    const isFocused = document.activeElement && document.activeElement.id === 'user-search';
    const cursorStart = isFocused ? searchEl.selectionStart : null;
    const cursorEnd = isFocused ? searchEl.selectionEnd : null;

    const list = filtered();
    const working = users.filter(u => u.user_type === 'intern' && u.working_status === 'Working').length;
    area.innerHTML = `
<div class="section-header">
  <div class="section-title"><i class="bi bi-people-fill text-danger"></i> Danh sách thực tập sinh</div>
  <div>
    <button class="btn btn-outline-success btn-sm me-2" onclick="downloadImportTemplate()"><i class="bi bi-download me-1"></i>Tải mẫu Excel</button>
    <div class="dropdown d-inline-block me-2">
      <button class="btn btn-success btn-sm dropdown-toggle" type="button" data-bs-toggle="dropdown" aria-expanded="false">
        <i class="bi bi-upload me-1"></i>Nhập dữ liệu
      </button>
      <ul class="dropdown-menu shadow">
        <li><a class="dropdown-item" href="#" onclick="event.preventDefault(); document.getElementById('import-file-input').click()"><i class="bi bi-file-earmark-excel me-2 text-success"></i>Nhập từ Excel</a></li>
        <li><a class="dropdown-item" href="#" onclick="event.preventDefault(); promptImportLink()"><i class="bi bi-link-45deg me-2 text-primary"></i>Nhập từ link sheet</a></li>
      </ul>
    </div>
    <input type="file" id="import-file-input" class="d-none" accept=".xlsx" onchange="handleImportExcel(event)" />
    <button class="btn btn-primary btn-sm" id="btn-add-user"><i class="bi bi-person-plus-fill me-1"></i>Thêm mới</button>
  </div>
</div>
<div class="filter-bar mb-3">
  <input class="form-control" id="user-search" placeholder="Tìm kiếm tên, mã NV, dự án..." value="${filter}" style="max-width:280px">
  <select id="filter-status" class="form-select" style="max-width:150px">
    <option value="">Tất cả trạng thái</option>
    <option value="Working" ${filterStatus === 'Working' ? 'selected' : ''}>Đang làm</option>
    <option value="Resigned" ${filterStatus === 'Resigned' ? 'selected' : ''}>Đã nghỉ</option>
  </select>
  <span class="ms-auto small text-muted align-self-center">
    <strong class="text-success">${working}</strong> đang làm · <strong>${list.length}</strong> hiển thị
  </span>
</div>
<div class="table-wrap">
  <div class="table-responsive">
    <table class="table table-hover">
      <thead><tr>
        <th>Mã NV</th><th>Họ tên</th><th>Dự án</th><th>Số ngày đã làm</th><th>Loại nhân sự</th>
        <th>Trợ cấp</th><th>Trạng thái</th><th>Vị trí</th><th style="width:100px">Thao tác</th>
      </tr></thead>
      <tbody>
        ${list.length ? list.map(u => `
        <tr>
          <td><code>${u.employee_code}</code></td>
          <td>
            <strong>${u.full_name}</strong><br>
            <small class="text-muted">${u.position || '—'} · ${u.viettel_email || '—'}</small>
          </td>
          <td>${u.project || '—'}</td>
          <td>${(() => {
            if (!u.join_date) return '—';
            const diff = new Date() - new Date(u.join_date);
            return Math.max(0, Math.floor(diff / (1000*60*60*24))) + ' ngày';
          })()}</td>
          <td>${badgeEmpType(u.employee_type)}</td>
          <td>${u.allowance || 'Không'}</td>
          <td>${badgeStatus(u.working_status)}</td>
          <td>${u.position || '—'}</td>
          <td>
            <button class="btn-icon edit me-1" title="Sửa" onclick="openEditUser(${u.id})"><i class="bi bi-pencil-fill"></i></button>
            <button class="btn-icon" style="background:rgba(248,81,73,.1);color:var(--danger)" title="Xóa" onclick="deleteUser(${u.id},'${u.full_name}')">
              <i class="bi bi-trash-fill"></i>
            </button>
          </td>
        </tr>`).join('') : '<tr><td colspan="8" class="text-center py-4 text-muted">Không có dữ liệu</td></tr>'}
      </tbody>
    </table>
  </div>
</div>`;

    document.getElementById('btn-add-user').onclick = () => openUserModal(null);
    document.getElementById('user-search').addEventListener('input', e => { filter = e.target.value.toLowerCase(); render(); });
    document.getElementById('filter-status').addEventListener('change', e => { filterStatus = e.target.value; render(); });

    if (isFocused) {
      const newSearchEl = document.getElementById('user-search');
      newSearchEl.focus();
      newSearchEl.setSelectionRange(cursorStart, cursorEnd);
    }
  }
  render();
}

window.openEditUser = async (id) => {
  const users = await api('GET', '/admin/users');
  openUserModal(users.find(u => u.id === id));
};

window.deleteUser = async (id, name) => {
  if (!confirm(`⚠️ Xác nhận XÓA tài khoản "${name}"?\n\nHành động này không thể hoàn tác và sẽ xóa toàn bộ lịch liên quan.`)) return;
  try {
    const r = await api('DELETE', `/admin/users/${id}`);
    toast(r.message, 'success');
    navigate('manage-users');
  } catch (e) { toast(e.message, 'error'); }
};

function openUserModal(user) {
  document.getElementById('modal-user-title').textContent = user ? 'Chỉnh sửa thực tập sinh' : 'Thêm thực tập sinh';
  const setV = (id, val) => { const el = document.getElementById(id); if (el) el.value = val ?? ''; };
  setV('user-id', user?.id || '');
  setV('u-code', user?.employee_code || ''); document.getElementById('u-code').disabled = !!user;
  setV('u-name', user?.full_name || '');
  setV('u-pass', '');
  setV('u-role', user?.role || 'intern');
  setV('u-gender', user?.gender || '');
  setV('u-birthday', user?.birthday || '');
  setV('u-ethnicity', user?.ethnicity || '');
  setV('u-viettel-email', user?.viettel_email || '');
  setV('u-phone', user?.phone || '');
  setV('u-cccd', user?.cccd || '');
  setV('u-hometown', user?.hometown || '');
  setV('u-bank-name', user?.bank_name || '');
  setV('u-bank-account', user?.bank_account || '');
  setV('u-project', user?.project || '');
  setV('u-position', user?.position || '');
  setV('u-join-date', user?.join_date || '');
  setV('u-allowance', user?.allowance || 'Không');
  setV('u-emp-type', user?.employee_type || 'TTS Trung tâm');
  setV('u-work-status', user?.working_status || 'Working');
  setV('u-emp-kind', user?.employment_type || 'Fulltime');
  bootstrap.Modal.getOrCreateInstance(document.getElementById('modal-user')).show();
}

document.getElementById('btn-save-user').addEventListener('click', async () => {
  const id = document.getElementById('user-id').value;
  const getV = (eid) => document.getElementById(eid).value.trim() || null;
  const payload = {
    full_name: getV('u-name'),
    role: getV('u-role'),
    gender: getV('u-gender'),
    birthday: getV('u-birthday'),
    ethnicity: getV('u-ethnicity'),
    viettel_email: getV('u-viettel-email'),
    phone: getV('u-phone'),
    cccd: getV('u-cccd'),
    hometown: getV('u-hometown'),
    bank_name: getV('u-bank-name'),
    bank_account: getV('u-bank-account'),
    project: getV('u-project'),
    position: getV('u-position'),
    join_date: getV('u-join-date'),
    allowance: getV('u-allowance') || 'Không',
    employee_type: getV('u-emp-type'),
    working_status: getV('u-work-status'),
    employment_type: getV('u-emp-kind'),
  };
  try {
    if (id) {
      await api('PUT', `/admin/users/${id}`, payload);
      toast('Cập nhật thành công');
    } else {
      await api('POST', '/admin/users', {
        employee_code: document.getElementById('u-code').value.trim(),
        ...payload,
      });
      toast('Đã thêm thực tập sinh');
    }
    bootstrap.Modal.getInstance(document.getElementById('modal-user')).hide();
    navigate('manage-users');
  } catch (e) { toast(e.message, 'error'); }
});

window.downloadImportTemplate = function () {
  const url = API + '/admin/users/import-template';
  const a = document.createElement('a');
  a.style.display = 'none';
  a.href = url;
  if (STATE.token) {
    // If backend requires auth for download, we should fetch as blob
    fetch(url, { headers: { 'Authorization': 'Bearer ' + STATE.token } })
      .then(res => res.blob())
      .then(blob => {
        const blobUrl = window.URL.createObjectURL(blob);
        a.href = blobUrl;
        a.download = 'template_import_tts.xlsx';
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(blobUrl);
      }).catch(err => toast('Lỗi khi tải mẫu', 'error'));
  } else {
    document.body.appendChild(a);
    a.click();
  }
};

window.handleImportExcel = async function (event) {
  const file = event.target.files[0];
  if (!file) return;

  const formData = new FormData();
  formData.append('file', file);

  toast('Đang xử lý file...', 'info');
  event.target.value = ''; // Reset input

  try {
    const res = await fetch(API + '/admin/users/import', {
      method: 'POST',
      headers: { 'Authorization': 'Bearer ' + STATE.token },
      body: formData
    });

    const data = await res.json();
    if (!res.ok) throw new Error(data.detail || 'Lỗi khi nhập dữ liệu');

    toast(data.message, data.success > 0 ? 'success' : 'info');
    navigate('manage-users'); // Reload table
  } catch (err) {
    toast(err.message, 'error');
  }
};

window.promptImportLink = async function () {
  const url = prompt("Nhập đường dẫn Google Sheets\\n(Lưu ý: File Google Sheets cần được chia sẻ ở chế độ 'Bất kỳ ai có đường liên kết đều có thể xem'):");
  if (!url) return;
  if (!url.includes('docs.google.com/spreadsheets')) {
    toast('Đường dẫn không hợp lệ', 'error');
    return;
  }

  toast('Đang xử lý dữ liệu từ link...', 'info');
  try {
    const res = await api('POST', '/admin/users/import-link', { url: url });
    toast(res.message, res.success > 0 ? 'success' : 'info');
    navigate('manage-users');
  } catch (err) {
    toast(err.message, 'error');
  }
};
