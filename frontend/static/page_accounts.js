// ═══════════════════════════════════════════
// MANAGE ACCOUNTS (Admin)
// ═══════════════════════════════════════════
async function renderManageAccounts(area) {
  let accounts = await api('GET', '/admin/accounts');
  let filter = '';

  function filtered() {
    return accounts.filter(a => 
      (a.full_name.toLowerCase().includes(filter) ||
       a.employee_code.toLowerCase().includes(filter) ||
       (a.username || '').toLowerCase().includes(filter))
    );
  }

  function render() {
    const list = filtered();
    area.innerHTML = `
<div class="section-header">
  <div class="section-title"><i class="bi bi-person-lines-fill text-danger"></i> Tổng hợp tài khoản</div>
  <button class="btn btn-success btn-sm" onclick="exportAccounts()"><i class="bi bi-file-earmark-excel-fill me-1"></i>Xuất Excel</button>
</div>
<div class="filter-bar mb-3">
  <input class="form-control" id="account-search" placeholder="🔍 Tìm kiếm tên, mã NV, tên đăng nhập..." value="${filter}" style="max-width:350px">
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
  }
  render();
}

window.exportAccounts = function() {
  fetch(API + '/admin/accounts/export', {
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
  if (!confirm(`Đặt lại mật khẩu về "123456" cho ${name}?`)) return;
  try { const r = await api('PATCH', `/admin/users/${id}/reset-password`); toast(r.message); }
  catch (e) { toast(e.message, 'error'); }
};
