// ═══════════════════════════════════════════════════════
// MANAGE EMPLOYEES (Admin)
// ═══════════════════════════════════════════════════════

async function renderManageEmployees(area) {
  let employees = await api('GET', '/employees/');
  let filter = '';
  
  // Use sets for multi-select filters. Empty set means no filter (show all).
  if (!window.empFilterPosition) window.empFilterPosition = new Set();
  if (!window.empFilterStatus) window.empFilterStatus = new Set();
  if (!window.empFilterCat) window.empFilterCat = new Set();

  function filtered() {
    return employees.filter(e => {
      const pos = e.position_name || e.position || '—';
      const st = e.employment_status || 'Thử việc';
      const cat = e.staff_category || 'NS trung tâm';

      const matchPos = window.empFilterPosition.size === 0 || window.empFilterPosition.has(pos);
      const matchSt = window.empFilterStatus.size === 0 || window.empFilterStatus.has(st);
      const matchCat = window.empFilterCat.size === 0 || window.empFilterCat.has(cat);

      const matchSearch = e.full_name.toLowerCase().includes(filter) ||
        e.employee_code.toLowerCase().includes(filter) ||
        (e.project || '').toLowerCase().includes(filter) ||
        pos.toLowerCase().includes(filter);

      return matchPos && matchSt && matchCat && matchSearch;
    });
  }

  function render() {
    const searchEl = document.getElementById('emp-search');
    const isFocused = document.activeElement?.id === 'emp-search';
    const cursorStart = isFocused ? searchEl?.selectionStart : null;
    const cursorEnd = isFocused ? searchEl?.selectionEnd : null;

    const list = filtered();
    
    // Extract unique values for filters
    const uniquePositions = [...new Set(employees.map(e => e.position_name || e.position || '—'))].sort();
    const uniqueStatuses = [...new Set(employees.map(e => e.employment_status || 'Thử việc'))].sort();
    const uniqueCategories = [...new Set(employees.map(e => e.staff_category || 'NS trung tâm'))].sort();

    const renderFilter = (colName, uniqueValues, activeSet, varName) => {
      const isActive = activeSet.size > 0;
      const isAllChecked = activeSet.size === 0;
      let html = `<th class="dropdown">
        <div class="d-inline-flex align-items-center">
          <span>${colName}</span>
          <span class="filter-icon-btn ${isActive ? 'active' : ''}" data-bs-toggle="dropdown" data-bs-auto-close="outside" title="Lọc ${colName}">
            <i class="bi bi-funnel"></i>
          </span>
          <ul class="dropdown-menu table-filter-menu shadow-sm" style="min-width: 180px; max-height: 280px; overflow-y: auto;">
            <li>
              <label class="dropdown-item d-flex align-items-center" style="cursor:pointer">
                <input type="checkbox" class="form-check-input me-2" onchange="toggleAllEmpFilter('${varName}', this.checked)" ${isAllChecked ? 'checked' : ''}> 
                <span class="filter-text-all">(Tất cả)</span>
              </label>
            </li>
            <li><hr class="dropdown-divider my-1"></li>`;
      
      for (const val of uniqueValues) {
        const checked = activeSet.has(val);
        const safeVal = val.replace(/'/g, "\\'").replace(/"/g, '&quot;');
        html += `<li>
            <label class="dropdown-item d-flex align-items-center" style="cursor:pointer">
              <input type="checkbox" class="form-check-input me-2" value="${safeVal}" onchange="toggleEmpFilter('${varName}', this.value, this.checked)" ${checked ? 'checked' : ''}>
              <span>${safeVal}</span>
            </label>
          </li>`;
      }
      html += `</ul></div></th>`;
      return html;
    };

    area.innerHTML = `
<div class="section-header">
  <div class="section-title"><i class="bi bi-people-fill text-danger"></i> Danh sách nhân viên</div>
  <div class="d-flex gap-2 flex-wrap">
    <button class="btn btn-outline-secondary btn-sm" onclick="downloadEmployeeTemplate()">
      <i class="bi bi-download me-1"></i>Tải mẫu Excel
    </button>
    <div class="dropdown d-inline-block">
      <button class="btn btn-outline-danger btn-sm dropdown-toggle" type="button" data-bs-toggle="dropdown">
        <i class="bi bi-upload me-1"></i>Nhập dữ liệu
      </button>
      <ul class="dropdown-menu shadow">
        <li><a class="dropdown-item" href="#" onclick="event.preventDefault(); document.getElementById('emp-import-file').click()">
          <i class="bi bi-file-earmark-excel me-2 text-danger"></i>Nhập từ Excel</a></li>
        <li><a class="dropdown-item" href="#" onclick="event.preventDefault(); promptEmployeeImportLink()">
          <i class="bi bi-link-45deg me-2 text-primary"></i>Nhập từ link sheet</a></li>
      </ul>
    </div>
    <input type="file" id="emp-import-file" class="d-none" accept=".xlsx" onchange="handleEmployeeImportExcel(event)" />
    <button class="btn btn-outline-success btn-sm" onclick="exportEmployees()">
      <i class="bi bi-file-earmark-arrow-down me-1"></i>Xuất Excel
    </button>
    <button class="btn btn-danger btn-sm" id="btn-add-employee">
      <i class="bi bi-person-plus-fill me-1"></i>Thêm mới
    </button>
  </div>
</div>

<div class="filter-bar mb-3">
  <input class="form-control" id="emp-search" placeholder="Tìm tên, mã NV, dự án, vị trí..." value="${filter}" style="max-width:300px">
  <button class="btn btn-outline-secondary btn-sm" id="btn-reset-emp-filter" title="Khôi phục bộ lọc">
    <i class="bi bi-arrow-counterclockwise me-1"></i>Khôi phục bộ lọc
  </button>
  <span class="ms-auto small text-muted align-self-center">
    <strong>${list.length}</strong> nhân viên hiển thị
  </span>
</div>

<div class="table-wrap">
  <div class="table-responsive" style="min-height: 400px;">
    <table class="table table-hover">
      <thead><tr>
        <th>Mã NV</th><th>Họ tên</th>
        ${renderFilter('Vị trí', uniquePositions, window.empFilterPosition, 'empFilterPosition')}
        <th>Dự án</th>
        <th>Quản lý</th>
        ${renderFilter('Tình trạng', uniqueStatuses, window.empFilterStatus, 'empFilterStatus')}
        ${renderFilter('Loại NS', uniqueCategories, window.empFilterCat, 'empFilterCat')}
        <th style="width:90px">Thao tác</th>
      </tr></thead>
      <tbody>
        ${list.length ? list.map(e => `
        <tr>
          <td><code>${e.employee_code}</code></td>
          <td>
            <strong>${e.full_name}</strong><br>
            <small class="text-muted">${e.viettel_email || '—'}</small>
          </td>
          <td>${(e.position_name || e.position) ? `<span class="role-pill">${e.position_name || e.position}</span>` : '—'}</td>
          <td>${e.project || '—'}</td>
          <td>${e.direct_manager || '—'}</td>
          <td>${badgeEmpStatus(e.employment_status)}</td>
          <td>${badgeStaffCat(e.staff_category)}</td>
          <td>
            <button class="btn-icon edit me-1" title="Sửa" onclick="openEditEmployee(${e.id})">
              <i class="bi bi-pencil-fill"></i>
            </button>
            <button class="btn-icon" style="background:rgba(248,81,73,.1);color:var(--danger)" title="Xóa"
              onclick="deleteEmployee(${e.id},'${e.full_name.replace(/'/g, "\\\\'")}')">
              <i class="bi bi-trash-fill"></i>
            </button>
          </td>
        </tr>`).join('') : '<tr><td colspan="8" class="text-center py-4 text-muted">Không có dữ liệu</td></tr>'}
      </tbody>
    </table>
  </div>
</div>`;

    document.getElementById('btn-add-employee').onclick = () => openEmployeeModal(null);
    document.getElementById('emp-search').addEventListener('input', e => { filter = e.target.value.toLowerCase(); render(); });
    document.getElementById('btn-reset-emp-filter').onclick = () => {
      filter = '';
      window.empFilterPosition = new Set();
      window.empFilterStatus = new Set();
      window.empFilterCat = new Set();
      render();
    };

    if (isFocused) {
      const el = document.getElementById('emp-search');
      el?.focus();
      el?.setSelectionRange(cursorStart, cursorEnd);
    }
  }

  window.toggleEmpFilter = (varName, val, isChecked) => {
    const activeSet = window[varName];
    if (isChecked) {
      activeSet.add(val);
    } else {
      activeSet.delete(val);
    }
    render();
  };

  window.toggleAllEmpFilter = (varName, isChecked) => {
    window[varName].clear();
    render();
  };

  render();
}

// ─── Badge helpers ────────────────────────────────────────────────────────────

function badgeEmpStatus(s) {
  const text = s || 'Thử việc';
  const lower = text.toLowerCase();
  if (lower.includes('chuyển trung tâm')) {
    return `<span class="custom-badge" style="background:rgba(156,39,176,.15);color:#9c27b0;border:1px solid rgba(156,39,176,.3)">${text}</span>`;
  }
  if (lower.includes('chính thức')) {
    return `<span class="custom-badge" style="background:rgba(63,185,80,.15);color:var(--success);border:1px solid rgba(63,185,80,.3)">${text}</span>`;
  }
  return `<span class="custom-badge" style="background:rgba(255,193,7,.15);color:#d39e00;border:1px solid rgba(255,193,7,.3)">${text}</span>`;
}

function badgeStaffCat(c) {
  if (c === 'Cho mượn') return `<span class="custom-badge" style="background:rgba(229,57,53,.12);color:var(--danger);border:1px solid rgba(229,57,53,.3)">Cho mượn</span>`;
  if (c === 'Onsite') return `<span class="custom-badge" style="background:rgba(88,166,255,.15);color:var(--info);border:1px solid rgba(88,166,255,.3)">Onsite</span>`;
  return `<span class="custom-badge" style="background:rgba(88,166,255,.1);color:var(--info);border:1px solid rgba(88,166,255,.2)">NS trung tâm</span>`;
}

// ─── Open modal ───────────────────────────────────────────────────────────────

window.openEditEmployee = async (id) => {
  const employees = await api('GET', '/employees/');
  openEmployeeModal(employees.find(e => e.id === id));
};

async function openEmployeeModal(emp) {
  // Load positions and managers
  const [positions, managers] = await Promise.all([
    api('GET', '/employees/positions'),
    api('GET', '/employees/managers'),
  ]);

  // Populate position dropdown
  const posEl = document.getElementById('emp-position-id');
  posEl.innerHTML = '<option value="">-- Chọn vị trí --</option>' +
    positions.map(p => `<option value="${p.id}" ${emp?.position_id === p.id ? 'selected' : ''}>${p.name}</option>`).join('');

  // Populate manager dropdown
  const mgrEl = document.getElementById('emp-manager');
  mgrEl.innerHTML = '<option value="">-- Chọn quản lý --</option>' +
    managers.map(m => `<option value="${m.username || ''}" ${emp?.direct_manager === m.username ? 'selected' : ''}>${m.full_name}${m.position_name ? ' (' + m.position_name + ')' : ''}</option>`).join('');

  // Populate borrow PM dropdown (same list)
  const borrowPmEl = document.getElementById('emp-borrow-pm');
  borrowPmEl.innerHTML = '<option value="">-- Chọn PM --</option>' +
    managers.map(m => `<option value="${m.username || ''}" ${emp?.borrow_pm === m.username ? 'selected' : ''}>${m.full_name}${m.position_name ? ' (' + m.position_name + ')' : ''}</option>`).join('');

  document.getElementById('modal-employee-title').textContent = emp ? 'Chỉnh sửa nhân viên' : 'Thêm nhân viên mới';

  const setV = (id, val) => { const el = document.getElementById(id); if (el) el.value = val ?? ''; };
  setV('emp-id', emp?.id || '');
  setV('emp-name', emp?.full_name || '');
  setV('emp-code', emp?.employee_code || '');
  if (emp) document.getElementById('emp-code').disabled = true;
  else document.getElementById('emp-code').disabled = false;
  setV('emp-project', emp?.project || '');
  setV('emp-gender', emp?.gender || '');
  setV('emp-ethnicity', emp?.ethnicity || '');
  setV('emp-email', emp?.viettel_email || '');
  setV('emp-birthday', emp?.birthday || '');
  setV('emp-hometown', emp?.hometown || '');
  setV('emp-phone', emp?.phone || '');
  setV('emp-cccd', emp?.cccd || '');
  setV('emp-serial', emp?.computer_serial || '');
  setV('emp-bank-account', emp?.bank_account || '');
  setV('emp-bank-name', emp?.bank_name || 'Viettel Money');
  setV('emp-emp-status', emp?.employment_status || 'Thử việc');
  setV('emp-mac', emp?.use_company_mac || 'Không');
  setV('emp-seat', emp?.seat_position || '');
  setV('emp-staff-cat', emp?.staff_category || 'NS trung tâm');
  setV('emp-borrow-end', emp?.borrow_end_date || '');
  setV('emp-borrow-project', emp?.borrow_project || '');
  setV('emp-borrow-center', emp?.borrow_center || '');

  // Manager dropdown value
  if (emp?.direct_manager) mgrEl.value = emp.direct_manager;
  if (emp?.borrow_pm) borrowPmEl.value = emp.borrow_pm;

  // Toggle borrow section
  handleStaffCategoryChange();

  bootstrap.Modal.getOrCreateInstance(document.getElementById('modal-employee')).show();
}

// ─── Toggle borrow fields ─────────────────────────────────────────────────────

window.handleStaffCategoryChange = function () {
  const cat = document.getElementById('emp-staff-cat')?.value;
  const borrowSection = document.getElementById('borrow-fields');
  if (borrowSection) {
    borrowSection.classList.toggle('d-none', cat !== 'Cho mượn');
  }
};

// ─── Save employee ────────────────────────────────────────────────────────────

document.getElementById('btn-save-employee').addEventListener('click', async () => {
  const id = document.getElementById('emp-id').value;
  const getV = eid => document.getElementById(eid)?.value?.trim() || null;
  const staffCat = getV('emp-staff-cat');

  const payload = {
    full_name: getV('emp-name'),
    gender: getV('emp-gender'),
    ethnicity: getV('emp-ethnicity'),
    viettel_email: getV('emp-email'),
    birthday: getV('emp-birthday'),
    hometown: getV('emp-hometown'),
    phone: getV('emp-phone'),
    cccd: getV('emp-cccd'),
    computer_serial: getV('emp-serial'),
    bank_account: getV('emp-bank-account'),
    bank_name: getV('emp-bank-name') || 'Viettel Money',
    project: getV('emp-project'),
    position_id: document.getElementById('emp-position-id').value ? parseInt(document.getElementById('emp-position-id').value) : null,
    direct_manager: document.getElementById('emp-manager').value || null,
    employment_status: getV('emp-emp-status') || 'Thử việc',
    use_company_mac: getV('emp-mac') || 'Không',
    seat_position: getV('emp-seat'),
    staff_category: staffCat || 'NS trung tâm',
    // Borrow fields – only if "Cho mượn"
    borrow_end_date: staffCat === 'Cho mượn' ? getV('emp-borrow-end') : null,
    borrow_project: staffCat === 'Cho mượn' ? getV('emp-borrow-project') : null,
    borrow_pm: staffCat === 'Cho mượn' ? (document.getElementById('emp-borrow-pm').value || null) : null,
    borrow_center: staffCat === 'Cho mượn' ? getV('emp-borrow-center') : null,
  };

  try {
    if (id) {
      await api('PUT', `/employees/${id}`, payload);
      toast('Cập nhật nhân viên thành công', 'success');
    } else {
      const empCode = document.getElementById('emp-code').value.trim();
      if (!empCode) { toast('Vui lòng nhập mã nhân viên', 'error'); return; }
      await api('POST', '/employees/', { employee_code: empCode, ...payload });
      toast('Đã thêm nhân viên và tạo tài khoản tự động', 'success');
    }
    bootstrap.Modal.getInstance(document.getElementById('modal-employee')).hide();
    navigate('manage-employees');
  } catch (e) { toast(e.message, 'error'); }
});

// ─── Delete employee ──────────────────────────────────────────────────────────

window.deleteEmployee = async (id, name) => {
  const ok = await showConfirm({
    title: 'Xóa thông tin nhân viên',
    message: `Xác nhận XÓA nhân viên "${name}"?\n\nHành động này không thể hoàn tác.`,
    okText: 'Xóa nhân viên',
    type: 'danger'
  });
  if (!ok) return;
  try {
    const r = await api('DELETE', `/employees/${id}`);
    toast(r.message, 'success');
    navigate('manage-employees');
  } catch (e) { toast(e.message, 'error'); }
};

// ─── Import / Export ──────────────────────────────────────────────────────────

window.downloadEmployeeTemplate = function () {
  const url = API + '/employees/import-template';
  fetch(url, { headers: { 'Authorization': 'Bearer ' + STATE.token } })
    .then(res => res.blob())
    .then(blob => {
      const blobUrl = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = blobUrl;
      a.download = 'template_import_nhansu.xlsx';
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(blobUrl);
    }).catch(() => toast('Lỗi khi tải mẫu', 'error'));
};

window.handleEmployeeImportExcel = async function (event) {
  const file = event.target.files[0];
  if (!file) return;
  const formData = new FormData();
  formData.append('file', file);
  toast('Đang xử lý file...', 'info');
  event.target.value = '';
  try {
    const res = await fetch(API + '/employees/import', {
      method: 'POST',
      headers: { 'Authorization': 'Bearer ' + STATE.token },
      body: formData,
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.detail || 'Lỗi khi nhập dữ liệu');
    showImportResult(data);
    navigate('manage-employees');
  } catch (err) {
    toast(err.message, 'error');
  }
};

window.promptEmployeeImportLink = async function () {
  const fn = window.showPrompt || showPrompt;
  const savedUrl = localStorage.getItem('last_emp_sheet_url') || localStorage.getItem('last_sheet_url') || '';
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
  localStorage.setItem('last_emp_sheet_url', url);
  localStorage.setItem('last_sheet_url', url);
  toast('Đang xử lý dữ liệu từ link...', 'info');
  try {
    const res = await api('POST', '/employees/import-link', { url });
    showImportResult(res);
    navigate('manage-employees');
  } catch (err) {
    toast(err.message, 'error');
  }
};

function showImportResult(data) {
  const lines = [`✅ Thành công: ${data.success} nhân viên`];
  if (data.skipped) lines.push(`❌ Bỏ qua (trùng mã): ${data.skipped} người`);
  if (data.renamed?.length) {
    data.renamed.forEach(r => lines.push(`⚠️ Đổi username: ${r.original} → ${r.actual}`));
  }
  // Show detailed result
  const detail = lines.join('\n');
  toast(data.message, data.success > 0 ? 'success' : 'info');
  if (data.renamed?.length || data.skipped) {
    showConfirm({
      title: 'Chi tiết kết quả import',
      message: detail,
      okText: 'Đã hiểu',
      cancelText: 'Đóng',
      type: 'info'
    });
  }
}

window.exportEmployees = function () {
  const url = API + '/employees/export';
  fetch(url, { headers: { 'Authorization': 'Bearer ' + STATE.token } })
    .then(res => res.blob())
    .then(blob => {
      const blobUrl = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = blobUrl;
      a.download = 'danh_sach_nhan_su.xlsx';
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(blobUrl);
    }).catch(() => toast('Lỗi khi xuất Excel', 'error'));
};
