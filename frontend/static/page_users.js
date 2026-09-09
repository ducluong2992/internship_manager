// ═══════════════════════════════════════════
// MANAGE USERS (Admin)
// ═══════════════════════════════════════════
async function renderManageUsers(area) {
  let users = await api('GET', '/admin/users');
  let filter = '';

  if (!window.userFilterProject) window.userFilterProject = new Set();
  if (!window.userFilterEmpType) window.userFilterEmpType = new Set();
  if (!window.userFilterAllowance) window.userFilterAllowance = new Set();
  if (!window.userFilterStatus) window.userFilterStatus = new Set();
  if (!window.userFilterPosition) window.userFilterPosition = new Set();

  function filtered() {
    return users.filter(u => {
      if (u.user_type !== 'intern') return false;

      // Search text
      const matchSearch = !filter || (
        u.full_name.toLowerCase().includes(filter) ||
        u.employee_code.toLowerCase().includes(filter) ||
        (u.project || '').toLowerCase().includes(filter) ||
        (u.position || '').toLowerCase().includes(filter) ||
        (u.employee_type || '').toLowerCase().includes(filter)
      );
      if (!matchSearch) return false;

      // 1. Dự án
      if (window.userFilterProject.size > 0) {
        const hasProj = u.project && u.project.trim() !== '' && u.project !== '—';
        let projMatch = false;
        if (window.userFilterProject.has('__IN_PROJECT__') && hasProj) projMatch = true;
        if (window.userFilterProject.has('__NO_PROJECT__') && !hasProj) projMatch = true;
        if (!projMatch) return false;
      }

      // 2. Loại Nhân Sự
      if (window.userFilterEmpType.size > 0) {
        const empTypeVal = u.employee_type || 'Khác';
        if (!window.userFilterEmpType.has(empTypeVal)) return false;
      }

      // 3. Trợ cấp
      if (window.userFilterAllowance.size > 0) {
        const hasAllowance = u.allowance && u.allowance !== 'Không' && u.allowance.trim() !== '';
        let allowMatch = false;
        if (window.userFilterAllowance.has('Có') && hasAllowance) allowMatch = true;
        if (window.userFilterAllowance.has('Không') && !hasAllowance) allowMatch = true;
        if (!allowMatch) return false;
      }

      // 4. Trạng thái
      if (window.userFilterStatus.size > 0) {
        const ws = (u.working_status || '').toLowerCase();
        const normWs = (ws.includes('nghỉ') || ws.includes('nghi') || ws.includes('dừng') || ws.includes('dung') || ws === 'resigned') ? 'Resigned' : 'Working';
        if (!window.userFilterStatus.has(u.working_status) && !window.userFilterStatus.has(normWs)) return false;
      }

      // 5. Vị trí
      if (window.userFilterPosition.size > 0) {
        const posVal = u.position || 'Chưa phân vị trí';
        if (!window.userFilterPosition.has(posVal)) return false;
      }

      return true;
    });
  }

  function render() {
    const searchEl = document.getElementById('user-search');
    const isFocused = document.activeElement && document.activeElement.id === 'user-search';
    const cursorStart = isFocused ? searchEl?.selectionStart : null;
    const cursorEnd = isFocused ? searchEl?.selectionEnd : null;

    const list = filtered();
    const working = users.filter(u => {
      if (u.user_type !== 'intern') return false;
      const ws = (u.working_status || '').toLowerCase();
      return ws === 'working' || ws.includes('đang làm') || ws.includes('dang lam') || (!ws.includes('nghỉ') && !ws.includes('nghi') && !ws.includes('resigned') && ws !== '');
    }).length;

    // Build filter options lists
    const projectOptions = [
      { key: '__IN_PROJECT__', label: 'Đang trong dự án' },
      { key: '__NO_PROJECT__', label: 'Không trong dự án' }
    ];

    const uniqueEmpTypes = [...new Set(users.filter(u => u.user_type === 'intern').map(u => u.employee_type || 'Khác'))].sort();
    const empTypeOptions = uniqueEmpTypes.map(t => ({ key: t, label: t }));

    const allowanceOptions = [
      { key: 'Có', label: 'Có trợ cấp' },
      { key: 'Không', label: 'Không trợ cấp' }
    ];

    const statusOptions = [
      { key: 'Working', label: 'Đang làm' },
      { key: 'Resigned', label: 'Đã nghỉ' }
    ];

    const uniquePositions = [...new Set(users.filter(u => u.user_type === 'intern').map(u => u.position || 'Chưa phân vị trí'))].sort();
    const positionOptions = uniquePositions.map(p => ({ key: p, label: p }));

    const renderFilterHeader = (colTitle, options, activeSet, varName) => {
      const isActive = activeSet.size > 0;
      const isAllChecked = activeSet.size === 0;
      let html = `<th class="dropdown">
        <div class="d-inline-flex align-items-center">
          <span>${colTitle}</span>
          <span class="filter-icon-btn ${isActive ? 'active' : ''}" 
                data-bs-toggle="dropdown" 
                data-bs-auto-close="outside"
                title="Lọc ${colTitle}">
            <i class="bi bi-funnel"></i>
          </span>
          <ul class="dropdown-menu table-filter-menu shadow-sm" style="min-width: 180px; max-height: 280px; overflow-y: auto;">
            <li>
              <label class="dropdown-item d-flex align-items-center" style="cursor:pointer">
                <input type="checkbox" class="form-check-input me-2" onchange="toggleAllUserFilter('${varName}', this.checked)" ${isAllChecked ? 'checked' : ''}> 
                <span class="filter-text-all">(Tất cả)</span>
              </label>
            </li>
            <li><hr class="dropdown-divider my-1"></li>`;
      
      options.forEach(opt => {
        const key = typeof opt === 'object' ? opt.key : opt;
        const label = typeof opt === 'object' ? opt.label : opt;
        const checked = activeSet.has(key);
        const safeKey = String(key).replace(/'/g, "\\'").replace(/"/g, '&quot;');
        const safeLabel = String(label).replace(/</g, '&lt;').replace(/>/g, '&gt;');
        html += `<li>
          <label class="dropdown-item d-flex align-items-center" style="cursor:pointer">
            <input type="checkbox" class="form-check-input me-2" value="${safeKey}" onchange="toggleUserFilter('${varName}', this.value, this.checked)" ${checked ? 'checked' : ''}>
            <span>${safeLabel}</span>
          </label>
        </li>`;
      });
      
      html += `</ul></div></th>`;
      return html;
    };

    area.innerHTML = `
<div class="section-header">
  <div class="section-title"><i class="bi bi-people-fill text-danger"></i> Danh sách thực tập sinh</div>
  <div>
    <button class="btn btn-outline-danger btn-sm me-2" onclick="downloadImportTemplate()"><i class="bi bi-download me-1"></i>Tải mẫu Excel</button>
    <div class="dropdown d-inline-block me-2">
      <button class="btn btn-outline-danger btn-sm dropdown-toggle" type="button" data-bs-toggle="dropdown" aria-expanded="false">
        <i class="bi bi-upload me-1"></i>Nhập dữ liệu
      </button>
      <ul class="dropdown-menu shadow">
        <li><a class="dropdown-item" href="#" onclick="event.preventDefault(); document.getElementById('import-file-input').click()"><i class="bi bi-file-earmark-excel me-2 text-danger"></i>Nhập từ Excel</a></li>
        <li><a class="dropdown-item" href="#" onclick="event.preventDefault(); promptImportLink()"><i class="bi bi-link-45deg me-2 text-primary"></i>Nhập từ link sheet</a></li>
      </ul>
    </div>
    <input type="file" id="import-file-input" class="d-none" accept=".xlsx" onchange="handleImportExcel(event)" />
    <button class="btn btn-danger btn-sm" id="btn-add-user"><i class="bi bi-person-plus-fill me-1"></i>Thêm mới</button>
    <button class="btn btn-outline-danger btn-sm" id="btn-delete-all-users" onclick="deleteAllUsers()" title="Xóa toàn bộ danh sách TTS">
      <i class="bi bi-trash3-fill me-1"></i>Xóa tất cả
    </button>
  </div>
</div>
<div class="filter-bar mb-3">
  <input class="form-control" id="user-search" placeholder="Tìm kiếm tên, mã NV, dự án..." value="${filter}" style="max-width:300px">
  <button class="btn btn-outline-secondary btn-sm" id="btn-reset-user-filter" title="Khôi phục bộ lọc">
    <i class="bi bi-arrow-counterclockwise me-1"></i>Khôi phục bộ lọc
  </button>
  <span class="ms-auto small text-muted align-self-center">
    <strong class="text-success">${working}</strong> đang làm · <strong>${list.length}</strong> hiển thị
  </span>
</div>
<div class="table-wrap">
  <div class="table-responsive" style="min-height: 400px;">
    <table class="table table-hover">
      <thead><tr>
        <th>Mã NV</th>
        <th>Họ tên</th>
        ${renderFilterHeader('Dự án', projectOptions, window.userFilterProject, 'userFilterProject')}
        <th>Số ngày đã làm</th>
        ${renderFilterHeader('Loại nhân sự', empTypeOptions, window.userFilterEmpType, 'userFilterEmpType')}
        ${renderFilterHeader('Trợ cấp', allowanceOptions, window.userFilterAllowance, 'userFilterAllowance')}
        ${renderFilterHeader('Trạng thái', statusOptions, window.userFilterStatus, 'userFilterStatus')}
        ${renderFilterHeader('Vị trí', positionOptions, window.userFilterPosition, 'userFilterPosition')}
        <th style="width:100px">Thao tác</th>
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
          <td>${u.position ? `<span class="role-pill">${u.position}</span>` : '—'}</td>
          <td>
            <button class="btn-icon edit me-1" title="Sửa" onclick="openEditUser(${u.id})"><i class="bi bi-pencil-fill"></i></button>
            <button class="btn-icon" style="background:rgba(248,81,73,.1);color:var(--danger)" title="Xóa" onclick="deleteUser(${u.id},'${u.full_name}')">
              <i class="bi bi-trash-fill"></i>
            </button>
          </td>
        </tr>`).join('') : '<tr><td colspan="9" class="text-center py-4 text-muted">Không có dữ liệu</td></tr>'}
      </tbody>
    </table>
  </div>
</div>`;

    document.getElementById('btn-add-user').onclick = () => openUserModal(null);
    document.getElementById('user-search').addEventListener('input', e => { filter = e.target.value.toLowerCase(); render(); });
    document.getElementById('btn-reset-user-filter').onclick = () => {
      filter = '';
      window.userFilterProject.clear();
      window.userFilterEmpType.clear();
      window.userFilterAllowance.clear();
      window.userFilterStatus.clear();
      window.userFilterPosition.clear();
      render();
    };

    if (isFocused) {
      const newSearchEl = document.getElementById('user-search');
      newSearchEl?.focus();
      newSearchEl?.setSelectionRange(cursorStart, cursorEnd);
    }
  }

  window.toggleUserFilter = (varName, val, isChecked) => {
    const activeSet = window[varName];
    if (isChecked) {
      activeSet.add(val);
    } else {
      activeSet.delete(val);
    }
    render();
  };

  window.toggleAllUserFilter = (varName, isChecked) => {
    window[varName].clear();
    render();
  };

  render();
}

window.openEditUser = async (id) => {
  const users = await api('GET', '/admin/users');
  openUserModal(users.find(u => u.id === id));
};

window.deleteUser = async (id, name) => {
  const ok = await showConfirm({
    title: 'Xóa tài khoản thực tập sinh',
    message: `Xác nhận XÓA tài khoản "${name}"?\n\nHành động này không thể hoàn tác và sẽ xóa toàn bộ lịch liên quan.`,
    okText: 'Xóa tài khoản',
    type: 'danger'
  });
  if (!ok) return;
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

  toast('Đang tải file lên máy chủ...', 'info');
  event.target.value = '';

  try {
    const res = await fetch(API + '/api/import-jobs/submit?userType=intern', {
      method: 'POST',
      headers: { 'Authorization': 'Bearer ' + STATE.token },
      body: formData
    });

    const data = await res.json();
    if (!res.ok) throw new Error(data.error || data.detail || data.message || 'Lỗi khi nhập dữ liệu');

    showAlert({
      title: 'Tiếp nhận import thành công',
      message: 'Yêu cầu import Thực tập sinh đã được tiếp nhận thành công và đang được xử lý ngầm (background). Bạn có thể tiếp tục công việc khác!',
      okText: 'Đã hiểu',
      type: 'success'
    });

    trackBackgroundImportJob(data.jobId, 'TTS', () => {
      const cur = window.currentPage || (typeof STATE !== 'undefined' && STATE.currentPage);
      if (cur === 'manage-users' || !cur || document.getElementById('user-search')) {
        navigate('manage-users');
      }
    });
  } catch (err) {
    toast(err.message || 'Lỗi khi tải file', 'error');
  }
};

window.promptImportLink = async function () {
  const fn = window.showPrompt || showPrompt;
  const savedUrl = localStorage.getItem('last_user_sheet_url') || localStorage.getItem('last_sheet_url') || '';
  const url = await fn({
    title: 'Nhập đường dẫn Google Sheets',
    message: 'Lưu ý: File Google Sheets cần được chia sẻ ở chế độ "Bất kỳ ai có đường liên kết đều có thể xem"',
    placeholder: 'https://docs.google.com/spreadsheets/d/...',
    defaultValue: savedUrl
  });
  if (!url) return;
  if (!url.includes('docs.google.com/spreadsheets')) {
    toast('Đường dẫn không hợp lệ', 'error');
    return;
  }

  localStorage.setItem('last_user_sheet_url', url);
  localStorage.setItem('last_sheet_url', url);

  toast('Đang gửi yêu cầu import link Google Sheets...', 'info');
  try {
    const data = await api('POST', '/api/import-jobs/submit-link?userType=intern', { url: url });

    showAlert({
      title: 'Tiếp nhận import thành công',
      message: 'Yêu cầu import Thực tập sinh từ Google Sheets đã được tiếp nhận thành công và đang được xử lý ngầm (background). Bạn có thể tiếp tục công việc khác!',
      okText: 'Đã hiểu',
      type: 'success'
    });

    trackBackgroundImportJob(data.jobId, 'TTS', () => {
      const cur = window.currentPage || (typeof STATE !== 'undefined' && STATE.currentPage);
      if (cur === 'manage-users' || !cur || document.getElementById('user-search')) {
        navigate('manage-users');
      }
    });
  } catch (err) {
    toast(err.message || 'Lỗi khi gửi yêu cầu import', 'error');
  }
};


// ─── Xóa toàn bộ danh sách TTS ──────────────────────────────────────────────
async function deleteAllUsers() {
  const confirmed = await showConfirm({
    title: 'Xóa toàn bộ danh sách TTS',
    message: 'Bạn có chắc chắn muốn xóa toàn bộ danh sách thực tập sinh? Hành động này không thể hoàn tác. Tất cả dữ liệu liên quan (lịch làm việc, OT, v.v.) cũng sẽ bị xóa.',
    okText: 'Xóa tất cả',
    cancelText: 'Hủy bỏ',
    type: 'danger'
  });
  if (!confirmed) return;

  const btn = document.getElementById('btn-delete-all-users');
  const originalHtml = btn ? btn.innerHTML : '';
  if (btn) { btn.disabled = true; btn.innerHTML = `<span class="spinner-border spinner-border-sm me-1"></span>Đang xóa...`; }

  try {
    const res = await api('DELETE', '/admin/users/all');
    toast(res.message || 'Đã xóa toàn bộ danh sách TTS thành công', 'success');
    navigate('manage-users');
  } catch (err) {
    toast(err.message || 'Xóa thất bại', 'error');
    if (btn) { btn.disabled = false; btn.innerHTML = originalHtml; }
  }
}
