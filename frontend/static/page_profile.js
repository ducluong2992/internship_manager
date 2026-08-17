// ═══════════════════════════════════════════
// USER PROFILE (Intern & Employee)
// ═══════════════════════════════════════════
async function renderProfile(area) {
  const user = await api('GET', '/users/me');
  const isEmployee = STATE.user_type === 'employee';

  // Lấy tên vị trí nếu là employee
  let positionLabel = user.position || '—';
  if (isEmployee && user.position_id) {
    try {
      const positions = await api('GET', '/employees/positions');
      const pos = positions.find(p => p.id === user.position_id);
      if (pos) positionLabel = pos.name;
    } catch (_) {}
  }

  area.innerHTML = `
<div class="section-header">
  <div class="section-title"><i class="bi bi-person-badge-fill text-info"></i> Hồ sơ cá nhân</div>
  <div>
    <button class="btn btn-warning btn-sm me-1" id="btn-edit-profile-mode">
      <i class="bi bi-pencil-square me-1"></i>Chỉnh sửa
    </button>
    <button class="btn btn-primary btn-sm d-none" id="btn-save-profile">
      <i class="bi bi-floppy-fill me-1"></i>Lưu thay đổi
    </button>
  </div>
</div>

<!-- Avatar / Info Card -->
<div class="profile-section mb-3">
  <div class="d-flex align-items-center gap-4 flex-wrap">
    <div style="width:80px;height:80px;border-radius:50%;
      background:linear-gradient(135deg,var(--primary),var(--primary-dark));
      display:flex;align-items:center;justify-content:center;
      font-size:2.2rem;color:#fff;font-weight:800;flex-shrink:0;
      box-shadow:0 4px 20px rgba(229,57,53,.4)">
      ${user.full_name.charAt(0).toUpperCase()}
    </div>
    <div>
      <h4 class="fw-bold mb-1">${user.full_name}</h4>
      <div class="small text-muted mb-2">
        <code>${user.employee_code}</code>
        ${user.viettel_email ? `<span class="ms-2">· ${user.viettel_email}</span>` : ''}
      </div>
      <div class="d-flex gap-2 flex-wrap">
        ${isEmployee
          ? `<span class="custom-badge" style="background:rgba(88,166,255,.15);color:var(--info);border:1px solid rgba(88,166,255,.3)">Nhân viên</span>
             ${user.employment_status === 'Chính thức'
               ? `<span class="custom-badge" style="background:rgba(63,185,80,.15);color:var(--success);border:1px solid rgba(63,185,80,.3)">Chính thức</span>`
               : `<span class="custom-badge" style="background:rgba(255,193,7,.15);color:#d39e00;border:1px solid rgba(255,193,7,.3)">Thử việc</span>`
             }
             ${positionLabel !== '—' ? `<span class="custom-badge" style="background:rgba(229,57,53,.1);color:var(--danger);border:1px solid rgba(229,57,53,.25)">${positionLabel}</span>` : ''}`
          : `${badgeStatus(user.working_status)}
             <span class="custom-badge" style="background:rgba(88,166,255,.15);color:var(--info);border:1px solid rgba(88,166,255,.3)">${user.employment_type || 'Fulltime'}</span>
             ${user.project ? `<span class="custom-badge" style="background:rgba(210,153,34,.15);color:var(--warning);border:1px solid rgba(210,153,34,.3)">${user.project}</span>` : ''}
             ${user.allowance === 'Có' ? `<span class="custom-badge" style="background:rgba(63,185,80,.15);color:var(--success);border:1px solid rgba(63,185,80,.3)">Có trợ cấp</span>` : ''}`
        }
      </div>
    </div>
  </div>
</div>

<!-- Editable fields -->
<div class="profile-section mb-3">
  <div class="profile-section-title"><i class="bi bi-person-lines-fill"></i> Thông tin cá nhân</div>
  <div class="row g-3">
    <div class="col-md-4">
      <label class="form-label">Số điện thoại</label>
      <input id="p-phone" class="form-control" value="${user.phone||''}" placeholder="0901..." disabled />
    </div>
    <div class="col-md-4">
      <label class="form-label">CCCD</label>
      <input id="p-cccd" class="form-control" value="${user.cccd||''}" placeholder="0123456789..." disabled />
    </div>
    <div class="col-md-4">
      <label class="form-label">Giới tính</label>
      <select id="p-gender" class="form-select" disabled>
        <option value="">-- Chọn --</option>
        ${['Nam','Nữ','Khác'].map(g=>`<option ${user.gender===g?'selected':''}>${g}</option>`).join('')}
      </select>
    </div>
    <div class="col-md-4">
      <label class="form-label">Ngày sinh</label>
      <input id="p-birthday" type="date" class="form-control" value="${user.birthday||''}" disabled />
    </div>
    <div class="col-md-4">
      <label class="form-label">Dân tộc</label>
      <input id="p-ethnicity" class="form-control" value="${user.ethnicity||''}" placeholder="Kinh, Tày..." disabled />
    </div>
    <div class="col-md-4">
      <label class="form-label">Quê quán</label>
      <input id="p-hometown" class="form-control" value="${user.hometown||''}" placeholder="Tỉnh/Thành phố..." disabled />
    </div>
    <div class="col-md-6">
      <label class="form-label">Email Viettel</label>
      <input id="p-viettel-email" type="email" class="form-control" value="${user.viettel_email||''}" placeholder="abc@viettel.com.vn" disabled />
    </div>
    ${!isEmployee ? `
    <div class="col-md-6">
      <label class="form-label">Loại hình</label>
      <select id="p-employment-type" class="form-select" disabled>
        <option value="Fulltime" ${(user.employment_type||'').toLowerCase()==='fulltime'?'selected':''}>Fulltime</option>
        <option value="Parttime" ${(user.employment_type||'').toLowerCase()==='parttime'?'selected':''}>Parttime</option>
      </select>
    </div>` : ''}
  </div>
</div>

<div class="profile-section mb-3">
  <div class="profile-section-title"><i class="bi bi-bank2"></i> Tài khoản ngân hàng</div>
  <div class="row g-3">
    <div class="col-md-6">
      <label class="form-label">Tên ngân hàng</label>
      <input id="p-bank-name" class="form-control" value="${user.bank_name||''}" placeholder="Vietcombank, BIDV, MB..." disabled />
    </div>
    <div class="col-md-6">
      <label class="form-label">Số tài khoản</label>
      <input id="p-bank-account" class="form-control" value="${user.bank_account||''}" placeholder="0123456789" disabled />
    </div>
  </div>
</div>

${isEmployee ? `
<div class="profile-section">
  <div class="profile-section-title"><i class="bi bi-building-gear"></i> Thông tin công việc <small class="fw-normal">(chỉ đọc)</small></div>
  <div class="row g-3">
    <div class="col-md-4">
      <label class="form-label">Vị trí</label>
      <input class="form-control" value="${positionLabel}" disabled />
    </div>
    <div class="col-md-4">
      <label class="form-label">Dự án</label>
      <input class="form-control" value="${user.project||'—'}" disabled />
    </div>
    <div class="col-md-4">
      <label class="form-label">Quản lý trực tiếp</label>
      <input class="form-control" value="${user.direct_manager||'—'}" disabled />
    </div>
    <div class="col-md-4">
      <label class="form-label">Tình trạng</label>
      <input class="form-control" value="${user.employment_status||'Thử việc'}" disabled />
    </div>
    <div class="col-md-4">
      <label class="form-label">Loại nhân sự</label>
      <input class="form-control" value="${user.staff_category||'—'}" disabled />
    </div>
    <div class="col-md-4">
      <label class="form-label">Vị trí ngồi</label>
      <input class="form-control" value="${user.seat_position||'—'}" disabled />
    </div>
  </div>
</div>` : `
<div class="profile-section">
  <div class="profile-section-title"><i class="bi bi-building-gear"></i> Thông tin công việc <small class="fw-normal">(chỉ đọc)</small></div>
  <div class="row g-3">
    <div class="col-md-4">
      <label class="form-label">Dự án</label>
      <input class="form-control" value="${user.project||'—'}" disabled />
    </div>
    <div class="col-md-4">
      <label class="form-label">Loại nhân sự</label>
      <input class="form-control" value="${user.employee_type||'—'}" disabled />
    </div>
    <div class="col-md-4">
      <label class="form-label">Trợ cấp hàng tháng</label>
      <input class="form-control" value="${user.allowance ? 'Có' : 'Không'}" disabled />
    </div>
  </div>
</div>`}`;

  const editableIds = [
    'p-phone', 'p-cccd', 'p-gender', 'p-birthday', 'p-ethnicity',
    'p-hometown', 'p-viettel-email', 'p-employment-type', 'p-bank-name', 'p-bank-account'
  ];

  const btnEditMode = document.getElementById('btn-edit-profile-mode');
  const btnSave = document.getElementById('btn-save-profile');

  btnEditMode.onclick = () => {
    editableIds.forEach(id => {
      const el = document.getElementById(id);
      if (el) el.disabled = false;
    });
    btnEditMode.classList.add('d-none');
    btnSave.classList.remove('d-none');
  };

  btnSave.onclick = async () => {
    const getV = id => document.getElementById(id)?.value?.trim() || null;
    btnSave.disabled = true; btnSave.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Đang lưu...';
    try {
      const payload = {
        phone: getV('p-phone'), cccd: getV('p-cccd'),
        gender: getV('p-gender'), birthday: getV('p-birthday'),
        ethnicity: getV('p-ethnicity'), hometown: getV('p-hometown'),
        viettel_email: getV('p-viettel-email'),
        bank_name: getV('p-bank-name'), bank_account: getV('p-bank-account'),
      };
      if (!isEmployee) {
        payload.employment_type = document.getElementById('p-employment-type')?.value || null;
      }
      await api('PUT', '/users/me', payload);
      toast('Cập nhật hồ sơ thành công', 'success');
      // Lock inputs again after saving
      editableIds.forEach(id => {
        const el = document.getElementById(id);
        if (el) el.disabled = true;
      });
      btnSave.classList.add('d-none');
      btnEditMode.classList.remove('d-none');
    } catch (e) { toast(e.message, 'error'); }
    finally {
      btnSave.disabled = false; btnSave.innerHTML = '<i class="bi bi-floppy-fill me-1"></i>Lưu thay đổi';
    }
  };
}
