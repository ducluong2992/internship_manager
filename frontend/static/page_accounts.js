// ═══════════════════════════════════════════
// MANAGE ACCOUNTS (Admin)
// ═══════════════════════════════════════════
async function renderManageAccounts(area, userType = 'intern') {
  let accounts = await api('GET', `/admin/accounts?user_type=${userType}`);
  let filter = '';

  function filtered() {
    return accounts.filter(a =>
    (a.full_name.toLowerCase().includes(filter) ||
      a.employee_code.toLowerCase().includes(filter) ||
      (a.username || '').toLowerCase().includes(filter))
    );
  }

  function render() {
    const searchEl = document.getElementById('account-search');
    const isFocused = document.activeElement && document.activeElement.id === 'account-search';
    const cursorStart = isFocused ? searchEl.selectionStart : null;
    const cursorEnd = isFocused ? searchEl.selectionEnd : null;

    const list = filtered();
    area.innerHTML = `
<div class="section-header">
  <div class="section-title"><i class="bi bi-person-lines-fill text-danger"></i> Quản lý tài khoản ${userType === 'intern' ? 'TTS' : 'Nhân viên'}</div>
  <div class="d-flex gap-2">
    ${userType === 'employee' ? `
    <button class="btn btn-outline-danger btn-sm" onclick="lockAllResignedAccounts('employee')">
      <i class="bi bi-lock-fill me-1"></i>Khóa TK NV nghỉ việc
    </button>` : ''}
    <button class="btn btn-outline-success btn-sm" onclick="exportAccounts('${userType}')">
      <i class="bi bi-file-earmark-excel-fill me-1"></i>Xuất Excel
    </button>
  </div>
</div>
<div class="filter-bar mb-3">
  <input class="form-control" id="account-search" placeholder="Tìm kiếm tên, mã NV, tên đăng nhập..." value="${filter}" style="max-width:350px">
  <span class="ms-auto small text-muted align-self-center">
    <strong>${list.length}</strong> hiển thị
  </span>
</div>
<div class="table-wrap">
  <div class="table-responsive">
    <table class="table table-hover">
      <thead><tr>
        <th>Mã NV</th><th>Họ tên</th><th>Tên đăng nhập</th><th>Trạng thái</th><th style="width:100px">Thao tác</th>
      </tr></thead>
      <tbody>
        ${list.length ? list.map(a => `
        <tr>
          <td><code>${a.employee_code}</code></td>
          <td><strong>${a.full_name}</strong></td>
          <td>${a.username ? `<code>${a.username}</code>` : '<span class="text-muted">Chưa tạo</span>'}</td>
          <td>${a.account_status
        ? '<span class="custom-badge" style="background:rgba(63,185,80,.15);color:#3fb950;border:1px solid rgba(63,185,80,.3)"><i class="bi bi-unlock-fill"></i> Mở</span>'
        : '<span class="custom-badge badge-locked"><i class="bi bi-lock-fill"></i> Khóa</span>'}</td>
          <td>
            <button class="btn-icon ${a.account_status ? 'lock' : 'unlock'} me-1"
              title="${a.account_status ? 'Khóa' : 'Mở khóa'}" onclick="toggleLockAccount(${a.user_id}, '${userType}')">
              <i class="bi bi-${a.account_status ? 'lock-fill' : 'unlock-fill'}"></i>
            </button>
            <button class="btn-icon reset me-1" title="Đặt lại mật khẩu (123456)" onclick="resetAccountPwd(${a.user_id},'${a.username || a.full_name}')">
              <i class="bi bi-arrow-counterclockwise"></i>
            </button>
          </td>
        </tr>`).join('') : '<tr><td colspan="5" class="text-center py-4 text-muted">Không có dữ liệu</td></tr>'}
      </tbody>
    </table>
  </div>
</div>`;

    document.getElementById('account-search').addEventListener('input', e => { filter = e.target.value.toLowerCase(); render(); });

    if (isFocused) {
      const newSearchEl = document.getElementById('account-search');
      newSearchEl.focus();
      newSearchEl.setSelectionRange(cursorStart, cursorEnd);
    }
  }
  render();
}

window.lockAllResignedAccounts = async function (userType = 'intern') {
  const label = userType === 'intern' ? 'Thực tập sinh' : 'Nhân viên';
  const ok = await showConfirm({
    title: 'Khóa tài khoản nhân sự đã nghỉ việc',
    message: `Bạn có chắc chắn muốn khóa tất cả tài khoản ${label} có tình trạng "Đã nghỉ việc"?`,
    okText: 'Khóa tất cả',
    type: 'danger'
  });
  if (!ok) return;

  try {
    const query = userType ? `?user_type=${userType}` : '';
    const res = await api('POST', `/admin/users/lock-resigned-accounts${query}`);
    toast(res.message, 'success');
    const targetPage = userType === 'employee' ? 'manage-emp-accounts' : 'manage-accounts';
    navigate(targetPage);
  } catch (err) {
    toast(err.message || 'Lỗi khi khóa tài khoản', 'error');
  }
};

window.exportAccounts = function (userType = 'intern') {
  fetch(API + `/admin/accounts/export?user_type=${userType}`, {
    headers: { 'Authorization': 'Bearer ' + STATE.token }
  })
    .then(res => {
      if (!res.ok) throw new Error('Lỗi khi xuất file');
      return res.blob();
    })
    .then(blob => {
      const blobUrl = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.style.display = 'none';
      a.href = blobUrl;
      a.download = 'danh_sach_tai_khoan.xlsx';
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(blobUrl);
    })
    .catch(err => toast(err.message, 'error'));
};

window.resetAccountPwd = async (id, name) => {
  const ok = await showConfirm({
    title: 'Đặt lại mật khẩu',
    message: `Đặt lại mật khẩu về "123456" cho ${name}?`,
    okText: 'Đặt lại mật khẩu',
    type: 'warning'
  });
  if (!ok) return;
  try { const r = await api('PATCH', `/admin/users/${id}/reset-password`); toast(r.message); }
  catch (e) { toast(e.message, 'error'); }
};

window.toggleLockAccount = async (id, userType = 'intern') => {
  try {
    const r = await api('PATCH', `/admin/users/${id}/lock`);
    toast(r.message, 'info');
    const targetPage = userType === 'employee' ? 'manage-emp-accounts' : 'manage-accounts';
    navigate(targetPage);
  } catch (e) { toast(e.message, 'error'); }
};
