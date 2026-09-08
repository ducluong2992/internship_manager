// ═══════════════════════════════════════════════════════
// MANAGE EMPLOYEES (Admin)
// ═══════════════════════════════════════════════════════

async function renderManageEmployees(area) {
  let employees = await api('GET', '/employees');
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
  const employees = await api('GET', '/employees');
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

  // Mode: Khóa form trước, yêu cầu bấm "Chỉnh sửa" mới cho phép lưu
  const formInputs = document.querySelectorAll('#form-employee input, #form-employee select');
  const btnEditMode = document.getElementById('btn-edit-employee-mode');
  const btnSave = document.getElementById('btn-save-employee');

  if (emp) {
    formInputs.forEach(el => el.disabled = true);
    if (btnEditMode) {
      btnEditMode.classList.remove('d-none');
      btnEditMode.onclick = () => {
        formInputs.forEach(el => {
          if (el.id !== 'emp-code') el.disabled = false;
        });
        btnEditMode.classList.add('d-none');
        if (btnSave) btnSave.classList.remove('d-none');
      };
    }
    if (btnSave) btnSave.classList.add('d-none');
  } else {
    formInputs.forEach(el => el.disabled = false);
    if (btnEditMode) btnEditMode.classList.add('d-none');
    if (btnSave) btnSave.classList.remove('d-none');
  }

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
      await api('POST', '/employees', { employee_code: empCode, ...payload });
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
  toast('Đang đối chiếu dữ liệu từ file Excel...', 'info');
  event.target.value = '';
  try {
    const res = await fetch(API + '/employees/preview-import-file', {
      method: 'POST',
      headers: { 'Authorization': 'Bearer ' + STATE.token },
      body: formData,
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.detail || data.message || 'Lỗi khi đọc dữ liệu');
    showEmployeeReconcileModal(data);
  } catch (err) {
    toast(err.message, 'error');
  }
};

let currentEmpReconcileData = null;

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
  toast('Đang đối chiếu dữ liệu từ link...', 'info');
  try {
    const data = await api('POST', '/employees/preview-import-link', { url });
    showEmployeeReconcileModal(data);
  } catch (err) {
    toast(err.message, 'error');
  }
};

function showEmployeeReconcileModal(data) {
  currentEmpReconcileData = data;
  const { updated, added, removed, unchanged_count, format_warnings } = data;

  const titleEl = document.getElementById('modal-reconcile-title');
  if (titleEl) titleEl.textContent = 'Kết quả đối chiếu dữ liệu Nhân viên';

  const warnContainer = document.getElementById('reconcile-warnings-container');
  if (warnContainer) {
    if (format_warnings && format_warnings.length > 0) {
      warnContainer.innerHTML = `
        <div class="alert alert-warning py-2 px-3 mb-3 border-warning-subtle text-dark rounded-2" style="font-size: 12px; background: #fff8e1;">
          <div class="fw-bold mb-1 text-warning-emphasis"><i class="bi bi-exclamation-triangle-fill me-1 text-warning"></i>Cảnh báo định dạng ô dữ liệu Excel:</div>
          <ul class="mb-0 ps-3">
            ${format_warnings.map(w => `<li>${w}</li>`).join('')}
          </ul>
        </div>`;
    } else {
      warnContainer.innerHTML = '';
    }
  }

  document.getElementById('rec-count-updated').textContent = updated.length;
  document.getElementById('rec-count-added').textContent = added.length;
  document.getElementById('rec-count-removed').textContent = removed.length;
  document.getElementById('rec-count-unchanged').textContent = unchanged_count || 0;

  document.getElementById('badge-count-updated').textContent = `${updated.length} người`;
  document.getElementById('badge-count-added').textContent = `${added.length} người`;
  document.getElementById('badge-count-removed').textContent = `${removed.length} người`;

  // Render Section 1: Updated
  const updatedContainer = document.getElementById('list-rec-updated');
  if (updated.length === 0) {
    updatedContainer.innerHTML = `<div class="text-muted text-center py-2 bg-light rounded" style="font-size: 12px;">Không có nhân viên thay đổi thông tin</div>`;
  } else {
    updatedContainer.innerHTML = updated.map(item => `
      <div class="border rounded p-2 bg-white" style="font-size: 12px;">
        <div class="d-flex align-items-center justify-content-between mb-1 pb-1 border-bottom" style="min-width: 0;">
          <div class="d-flex align-items-center gap-2 text-truncate me-2" style="min-width: 0; flex: 1;">
            <span class="badge bg-secondary font-monospace flex-shrink-0" style="font-size: 10px;">${item.employee_code}</span>
            <strong class="text-dark text-truncate" style="font-size: 13px;" title="${item.full_name}">${item.full_name}</strong>
          </div>
          <span class="text-muted flex-shrink-0" style="font-size: 11px;">${item.changes.length} thay đổi</span>
        </div>
        <div class="d-flex flex-column gap-1 pt-1">
          ${item.changes.map(ch => `
            <div class="p-1 px-2 rounded bg-light border d-flex align-items-center gap-2" style="font-size: 12px; min-width: 0;">
              <span class="fw-semibold text-secondary flex-shrink-0" style="min-width: 110px;">${ch.field_name}:</span>
              <span class="text-decoration-line-through text-muted text-truncate" style="max-width: 180px;" title="${ch.old_value || '—'}">${ch.old_value || '—'}</span>
              <i class="bi bi-arrow-right text-secondary fs-6 flex-shrink-0"></i>
              <span class="fw-bold text-dark text-truncate" style="max-width: 250px;" title="${ch.new_value}">${ch.new_value}</span>
            </div>
          `).join('')}
        </div>
      </div>
    `).join('');
  }

  // Render Section 2: Added
  const addedContainer = document.getElementById('list-rec-added');
  if (added.length === 0) {
    addedContainer.innerHTML = `<div class="text-muted text-center py-2 bg-light rounded" style="font-size: 12px;">Không có nhân viên thêm mới</div>`;
  } else {
    addedContainer.innerHTML = added.map(item => `
      <div class="border rounded p-2 bg-white" style="font-size: 12px;">
        <div class="d-flex align-items-center justify-content-between gap-2" style="min-width: 0;">
          <div class="d-flex align-items-center gap-2 text-truncate me-2" style="min-width: 0; flex: 1;">
            <span class="badge bg-secondary font-monospace flex-shrink-0" style="font-size: 10px;">${item.employee_code}</span>
            <strong class="text-dark text-truncate" style="font-size: 13px;" title="${item.full_name}">${item.full_name}</strong>
            <span class="badge bg-success flex-shrink-0" style="font-size: 10px;">Mới</span>
          </div>
          <div class="text-muted text-truncate text-end flex-shrink-0" style="font-size: 11px; max-width: 260px;" title="${[item.project, item.position, item.viettel_email].filter(Boolean).join(' · ')}">
            <span>${[item.project, item.position, item.viettel_email].filter(Boolean).join(' · ') || '—'}</span>
          </div>
        </div>
      </div>
    `).join('');
  }

  // Render Section 3: Removed (In Web but not in Sheet)
  const removedContainer = document.getElementById('list-rec-removed');
  if (removed.length === 0) {
    removedContainer.innerHTML = `<div class="text-muted text-center py-2 bg-light rounded" style="font-size: 12px;">Tất cả nhân viên trên Web đều có trong Sheet</div>`;
  } else {
    removedContainer.innerHTML = removed.map(item => `
      <div class="border rounded p-2 bg-white" style="font-size: 12px;">
        <div class="d-flex align-items-center justify-content-between gap-2" style="min-width: 0;">
          <div class="d-flex align-items-center gap-2 text-truncate me-2" style="min-width: 0; flex: 1;">
            <span class="badge bg-secondary font-monospace flex-shrink-0" style="font-size: 10px;">${item.employee_code}</span>
            <strong class="text-dark flex-shrink-0" style="font-size: 13px;">${item.full_name}</strong>
            <span class="text-muted text-truncate" style="font-size: 11px;" title="${[item.position, item.project].filter(Boolean).join(' · ')}">
              ${[item.position, item.project].filter(Boolean).join(' · ') ? '— ' + [item.position, item.project].filter(Boolean).join(' · ') : ''}
            </span>
          </div>
          <div class="choice-button-group d-flex align-items-center gap-1 flex-shrink-0">
            <input type="radio" class="btn-check" name="rec_removed_${item.id}" id="choice_keep_${item.id}" value="keep" checked>
            <label class="btn btn-outline-secondary btn-sm px-2 py-0" for="choice_keep_${item.id}" style="font-size: 11px;">Giữ lại</label>

            <input type="radio" class="btn-check" name="rec_removed_${item.id}" id="choice_delete_${item.id}" value="delete">
            <label class="btn btn-outline-danger btn-sm px-2 py-0" for="choice_delete_${item.id}" style="font-size: 11px;">Xóa đi</label>
          </div>
        </div>
      </div>
    `).join('');
  }

  // Set up confirm action button
  const confirmBtn = document.getElementById('btn-confirm-reconcile');
  confirmBtn.onclick = () => executeEmployeeReconcileSync(data);

  bootstrap.Modal.getOrCreateInstance(document.getElementById('modal-import-reconcile')).show();
}

async function executeEmployeeReconcileSync(data) {
  const deleteIds = [];
  if (data.removed && data.removed.length) {
    data.removed.forEach(item => {
      const deleteRadio = document.getElementById(`choice_delete_${item.id}`);
      if (deleteRadio && deleteRadio.checked) {
        deleteIds.push(item.id);
      }
    });
  }

  const payload = {
    updates: data.updated || [],
    additions: data.added || [],
    delete_ids: deleteIds
  };

  const confirmBtn = document.getElementById('btn-confirm-reconcile');
  const originalHtml = confirmBtn.innerHTML;
  confirmBtn.disabled = true;
  confirmBtn.innerHTML = `<span class="spinner-border spinner-border-sm me-2"></span>Đang cập nhật...`;

  try {
    const res = await api('POST', '/employees/confirm-import', payload);
    toast(res.message, 'success');
    bootstrap.Modal.getInstance(document.getElementById('modal-import-reconcile')).hide();
    navigate('manage-employees');
  } catch (err) {
    toast(err.message || 'Lỗi khi cập nhật dữ liệu', 'error');
  } finally {
    confirmBtn.disabled = false;
    confirmBtn.innerHTML = originalHtml;
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
