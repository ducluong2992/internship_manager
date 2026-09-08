/* ═══════════════════════════════════════════
   PAGE RENDERERS — appended to app.js scope
   ═══════════════════════════════════════════ */

// ── Utility ──
function fmtDate(d) { return d ? new Date(d).toLocaleDateString('vi-VN') : '—'; }
function fmtDateTime(d) { return d ? new Date(d).toLocaleString('vi-VN', { hour12: false }) : '—'; }
function fmtNum(n) { return n != null ? Number(n).toLocaleString('vi-VN') : '—'; }
function daysInMonth(y, m) { return new Date(y, m, 0).getDate(); }
function getWorkdays(y, m) {
  const days = [];
  const total = daysInMonth(y, m);
  for (let d = 1; d <= total; d++) {
    const dow = new Date(y, m - 1, d).getDay();
    if (dow !== 0 && dow !== 6) days.push(d);
  }
  return days;
}
function pad2(n) { return String(n).padStart(2, '0'); }
function toDateStr(y, m, d) { return `${y}-${pad2(m)}-${pad2(d)}`; }
function badgeStatus(s) {
  if (s === 'Working') return `<span class="custom-badge badge-working">Đang làm</span>`;
  if (s === 'Lên chính thức') return `<span class="custom-badge badge-purple">Lên chính thức</span>`;
  if (s === 'Chuyển trung tâm') return `<span class="custom-badge badge-blue">Chuyển TT</span>`;
  return `<span class="custom-badge badge-resigned">Đã nghỉ</span>`;
}
function badgeShift(s) {
  if (s === 'SC') return `<span class="custom-badge badge-working">SC</span>`;
  if (s === 'S')  return `<span class="custom-badge badge-blue">S</span>`;
  if (s === 'C')  return `<span class="custom-badge badge-yellow">C</span>`;
  return `<span style="opacity:.3">—</span>`;
}
function badgeEmpType(t) {
  if ((t||'').toLowerCase().includes('mượn')) return `<span class="custom-badge badge-yellow">Đi mượn</span>`;
  return `<span class="custom-badge badge-blue">TTS Trung tâm</span>`;
}
function badgeEmpKind(k) {
  if ((k||'').toLowerCase() === 'parttime') return `<span class="custom-badge badge-yellow">Part-time</span>`;
  return `<span class="custom-badge badge-working">Full-time</span>`;
}

function badgePeriod(s) {
  return s === 'open'
    ? `<span class="custom-badge badge-open">Mở</span>`
    : `<span class="custom-badge badge-closed">Đóng</span>`;
}

// ═══════════════════════════════════════════
// RENDER DISPATCHER
// ═══════════════════════════════════════════
async function renderPage(page, area) {
  try {
    switch (page) {
      case 'dashboard': await renderDashboard(area); break;
      case 'manage-users': await renderManageUsers(area); break;
      case 'manage-employees': await renderManageEmployees(area); break;
      case 'manage-periods': await renderManagePeriods(area); break;
      case 'manage-accounts': await renderManageAccounts(area, 'employee'); break;
      case 'manage-emp-accounts': await renderManageAccounts(area, 'employee'); break;
      case 'admin-schedule': await renderAdminSchedule(area); break;
      case 'profile': await renderProfile(area); break;
      case 'register-schedule': await renderRegisterSchedule(area); break;
      case 'view-schedule': await renderViewSchedule(area); break;
      case 'change-password': renderChangePassword(area); break;
      case 'documents': await renderDocuments(area); break;
      case 'ai-config': await renderAIConfig(area); break;
      case 'register-ot': await renderRegisterOT(area); break;
      case 'manage-ot': await renderManageOT(area); break;
      default: area.innerHTML = '<div class="empty-state"><i class="bi bi-compass"></i><p>Trang không tồn tại</p></div>';
    }
  } catch (err) {
    area.innerHTML = `<div class="alert alert-danger"><i class="bi bi-exclamation-triangle me-2"></i>${err.message}</div>`;
  }
}

// ═══════════════════════════════════════════
// DASHBOARD
// ═══════════════════════════════════════════
async function renderDashboard(area) {
  const [users, periods, stats] = await Promise.all([
    api('GET', '/admin/users'),
    api('GET', '/admin/periods'),
    api('GET', '/admin/stats'),
  ]);

  // ── OT Pending helpers (dashboard-scoped) ──
  async function loadOTPending() {
    try {
      const now2 = new Date();
      const params = new URLSearchParams({ month: now2.getMonth() + 1, year: now2.getFullYear(), status: 'Pending' });
      return await api('GET', `/overtime/admin/list?${params}`);
    } catch { return []; }
  }

  function renderOTPendingSection(pending) {
    if (!pending || pending.length === 0) {
      return `<tr><td colspan="6" class="text-center text-muted py-3"><i class="bi bi-check-circle me-2 text-success"></i>Không có yêu cầu OT nào đang chờ duyệt.</td></tr>`;
    }
    return pending.map(r => `
      <tr>
        <td><code style="font-size:0.78rem;color:#475569;background:#f1f5f9;padding:2px 6px;border-radius:4px">${r.employee_code}</code></td>
        <td class="fw-semibold" style="color:#1e293b">${r.full_name}</td>
        <td class="text-muted small">${r.project || '—'}</td>
        <td class="text-center small">${r.work_date ? r.work_date.split('-').reverse().join('/') : '—'}</td>
        <td class="text-center small">${r.start_time} – ${r.end_time}<br><small class="text-muted">${r.raw_hours}h</small></td>
        <td class="text-center" style="white-space:nowrap">
          <button class="btn btn-sm btn-success py-0 px-2 me-1" title="Duyệt nhanh" onclick="dashboardApproveOT(${r.id})">
            <i class="bi bi-check-lg"></i> Duyệt
          </button>
          <button class="btn btn-sm btn-outline-danger py-0 px-2" title="Từ chối" onclick="dashboardRejectOT(${r.id})">
            <i class="bi bi-x-lg"></i> Từ chối
          </button>
        </td>
      </tr>`).join('');
  }
  const now = new Date();
  const otPending = await loadOTPending();

  // Calculations for progress bars & cards
  const totalEmployees = stats.total_employees || 0;
  const activeInterns = stats.working || 0; // Chỉ lấy TỔNG SỐ TTS ACTIVE
  const totalStructure = totalEmployees + activeInterns; // Cơ cấu nhân sự hoạt động

  // 1. Cơ cấu Nhân sự
  const pctEmp = totalStructure > 0 ? Math.round((totalEmployees / totalStructure) * 100) : 0;
  const pctInternActive = totalStructure > 0 ? Math.round((activeInterns / totalStructure) * 100) : 0;

  // 2. Nhân sự trong Dự án / Không trong Dự án (Chỉ lấy của Nhân viên)
  const empInProj = stats.emp_in_project != null ? stats.emp_in_project : (users.filter(u => u.project && u.project.trim() !== '' && u.project !== '—').length);
  const empNoProj = stats.emp_no_project != null ? stats.emp_no_project : Math.max(0, totalEmployees - empInProj);
  const pctEmpInProj = totalEmployees > 0 ? Math.round((empInProj / totalEmployees) * 100) : 0;
  const pctEmpNoProj = totalEmployees > 0 ? Math.round((empNoProj / totalEmployees) * 100) : 0;

  // 3. Nhân sự theo Loại Nhân sự (Chỉ lấy của Nhân viên)
  const empTT = stats.emp_trung_tam || 0;
  const empOnsite = stats.emp_onsite || 0;
  const empBorrow = stats.emp_cho_muon || 0;
  const pctEmpTT = totalEmployees > 0 ? Math.round((empTT / totalEmployees) * 100) : 0;
  const pctEmpOnsite = totalEmployees > 0 ? Math.round((empOnsite / totalEmployees) * 100) : 0;
  const pctEmpBorrow = totalEmployees > 0 ? Math.round((empBorrow / totalEmployees) * 100) : 0;

  const renderStatBar = (label, count, total, percent, colors) => `
    <div class="mb-3">
      <div class="d-flex justify-content-between align-items-center mb-1">
        <span class="fw-semibold text-secondary small">${label}</span>
        <span class="fw-bold small text-dark">${count}/${total} <span class="text-muted fw-normal">(${percent}%)</span></span>
      </div>
      <div class="progress" style="height: 10px; border-radius: 6px; background: #e2e8f0;">
        <div class="progress-bar" role="progressbar" style="width: ${percent}%; background: linear-gradient(90deg, ${colors[0]}, ${colors[1]}); border-radius: 6px;" aria-valuenow="${percent}" aria-valuemin="0" aria-valuemax="100"></div>
      </div>
    </div>`;

  area.innerHTML = `
<div class="d-flex justify-content-between align-items-center mb-4">
  <h4 class="fw-bold mb-0 text-danger">Tổng quan Hệ thống</h4>
  <span class="badge bg-danger px-3 py-2">Tháng ${now.getMonth() + 1}/${now.getFullYear()}</span>
</div>

<!-- 1. KEY METRICS -->
<div class="row g-3 mb-4">
  <div class="col-12 col-sm-6 col-xl-3">
    <div class="viettel-stat-card card-red-1 shadow-sm">
      <div class="stat-title">TỔNG SỐ TTS ACTIVE</div>
      <div class="d-flex align-items-baseline">
        <span class="stat-number">${activeInterns}</span>
        <span class="stat-unit">nhân sự</span>
      </div>
    </div>
  </div>
  <div class="col-12 col-sm-6 col-xl-3">
    <div class="viettel-stat-card card-red-2 shadow-sm">
      <div class="stat-title">NV THỬ VIỆC / HỌC VIỆC</div>
      <div class="d-flex align-items-baseline">
        <span class="stat-number">${stats.emp_probation || 0}</span>
        <span class="stat-unit">nhân sự</span>
      </div>
    </div>
  </div>
  <div class="col-12 col-sm-6 col-xl-3">
    <div class="viettel-stat-card card-red-3 shadow-sm">
      <div class="stat-title">NV ONSITE / DỰ ÁN</div>
      <div class="d-flex align-items-baseline">
        <span class="stat-number">${empOnsite}</span>
        <span class="stat-unit">nhân sự</span>
      </div>
    </div>
  </div>
  <div class="col-12 col-sm-6 col-xl-3">
    <div class="viettel-stat-card card-red-4 shadow-sm">
      <div class="stat-title">NV ĐÃ NGHỈ VIỆC / SẮP OUT</div>
      <div class="d-flex align-items-baseline">
        <span class="stat-number">${stats.emp_resigned || 0}</span>
        <span class="stat-unit">nhân sự</span>
      </div>
    </div>
  </div>
</div>

<!-- 2. PROGRESS BAR STATS (3 TYPES) -->
<div class="row g-4 mb-4">
  <div class="col-md-4">
    <div class="glass-card p-4 h-100 d-flex flex-column">
      <h6 class="fw-bold mb-3"><i class="bi bi-pie-chart-fill text-danger me-2"></i>Cơ cấu Nhân sự</h6>
      <div class="flex-grow-1 d-flex flex-column justify-content-center pt-2">
        ${renderStatBar('Nhân viên', totalEmployees, totalStructure, pctEmp, ['#3B82F6', '#1D4ED8'])}
        ${renderStatBar('Thực tập sinh (Active)', activeInterns, totalStructure, pctInternActive, ['#06B6D4', '#0284C7'])}
      </div>
    </div>
  </div>
  <div class="col-md-4">
    <div class="glass-card p-4 h-100 d-flex flex-column">
      <h6 class="fw-bold mb-3"><i class="bi bi-briefcase-fill text-danger me-2"></i>Nhân viên trong / không trong Dự án</h6>
      <div class="flex-grow-1 d-flex flex-column justify-content-center pt-2">
        ${renderStatBar('Đang trong dự án', empInProj, totalEmployees, pctEmpInProj, ['#10B981', '#047857'])}
        ${renderStatBar('Không trong dự án', empNoProj, totalEmployees, pctEmpNoProj, ['#F59E0B', '#B45309'])}
      </div>
    </div>
  </div>
  <div class="col-md-4">
    <div class="glass-card p-4 h-100 d-flex flex-column">
      <h6 class="fw-bold mb-3"><i class="bi bi-diagram-3-fill text-danger me-2"></i>Nhân viên theo Loại Nhân sự</h6>
      <div class="flex-grow-1 d-flex flex-column justify-content-center pt-2">
        ${renderStatBar('NS Trung tâm', empTT, totalEmployees, pctEmpTT, ['#8B5CF6', '#6D28D9'])}
        ${renderStatBar('Onsite / Dự án', empOnsite, totalEmployees, pctEmpOnsite, ['#EC4899', '#BE185D'])}
        ${renderStatBar('Cho mượn / Đi mượn', empBorrow, totalEmployees, pctEmpBorrow, ['#F97316', '#C2410C'])}
      </div>
    </div>
  </div>
</div>

<!-- 3. OT PENDING APPROVAL -->
<div class="row mb-4">
  <div class="col-12">
    <div class="glass-card p-4">
      <div class="d-flex align-items-center justify-content-between mb-3">
        <h6 class="fw-bold mb-0">
          <i class="bi bi-clock-history text-warning me-2"></i>
          Yêu cầu OT đang chờ duyệt
          ${otPending.length > 0 ? `<span class="badge ms-2" style="background:linear-gradient(135deg,#f59e0b,#d97706);color:#fff;font-size:0.72rem;border-radius:20px;padding:3px 10px">${otPending.length}</span>` : ''}
        </h6>
        <a href="#" class="btn btn-sm btn-outline-danger px-3" onclick="navigate('manage-ot');return false;">
          <i class="bi bi-arrow-right me-1"></i>Xem tất cả
        </a>
      </div>
      <div class="table-responsive">
        <table class="table table-hover align-middle mb-0" id="dashboard-ot-pending-table">
          <thead class="table-light">
            <tr>
              <th>Mã NV</th>
              <th>Họ và tên</th>
              <th>Dự án</th>
              <th class="text-center">Ngày OT</th>
              <th class="text-center">Giờ OT</th>
              <th class="text-center">Thao tác nhanh</th>
            </tr>
          </thead>
          <tbody id="dashboard-ot-pending-tbody">
            ${renderOTPendingSection(otPending)}
          </tbody>
        </table>
      </div>
    </div>
  </div>
</div>

<!-- 4. TODAY WORKERS -->
<div class="row mb-4">
  <div class="col-12">
    <div class="glass-card p-4">
      <h6 class="fw-bold mb-3"><i class="bi bi-calendar-check-fill text-success me-2"></i>TTS đi làm hôm nay - ${pad2(now.getDate())}/${pad2(now.getMonth() + 1)}/${now.getFullYear()}</h6>
      <div class="table-responsive">
        <table class="table table-hover align-middle mb-0">
          <thead class="table-light">
            <tr>
              <th>Mã NV</th>
              <th>Họ và tên</th>
              <th>Ca làm việc</th>
            </tr>
          </thead>
          <tbody>
            ${stats.today_workers && stats.today_workers.length > 0 ? stats.today_workers.map(w => `
              <tr>
                <td><code>${w.employee_code}</code></td>
                <td class="fw-medium">${w.full_name}</td>
                <td>
                  ${w.shift === 'S' ? '<span class="custom-badge badge-blue">Sáng</span>' : ''}
                  ${w.shift === 'C' ? '<span class="custom-badge badge-yellow">Chiều</span>' : ''}
                  ${w.shift === 'SC' ? '<span class="custom-badge badge-working">Cả ngày</span>' : ''}
                </td>
              </tr>
            `).join('') : '<tr><td colspan="3" class="text-center text-muted py-3">Không có ai đăng ký lịch làm hôm nay.</td></tr>'}
          </tbody>
        </table>
      </div>
    </div>
  </div>
</div>
`;

  // ── Dashboard: register global approve/reject helpers ──
  window.dashboardApproveOT = async (id) => {
    const ok = await showConfirm({
      title: 'Duyệt yêu cầu OT',
      message: 'Xác nhận duyệt yêu cầu OT này?',
      okText: 'Duyệt OT',
      type: 'warning'
    });
    if (!ok) return;
    try {
      await api('POST', `/overtime/admin/${id}/approve`);
      toast('Đã duyệt OT!', 'success');
      // Refresh only the OT pending table
      const now2 = new Date();
      const params = new URLSearchParams({ month: now2.getMonth() + 1, year: now2.getFullYear(), status: 'Pending' });
      const updated = await api('GET', `/overtime/admin/list?${params}`);
      const tbody = document.getElementById('dashboard-ot-pending-tbody');
      if (tbody) tbody.innerHTML = renderOTPendingSection(updated);
      // Update badge
      const badge = document.querySelector('#dashboard-ot-pending-table')?.closest('.glass-card')?.querySelector('.badge');
      if (badge) {
        if (updated.length > 0) { badge.textContent = updated.length; }
        else { badge.style.display = 'none'; }
      }
    } catch (e) { toast(e.message, 'error'); }
  };

  window.dashboardRejectOT = (id) => {
    document.getElementById('reject-ot-id').value = id;
    document.getElementById('reject-reason-text').value = '';
    const modalEl = document.getElementById('modal-ot-reject');
    const modal = bootstrap.Modal.getOrCreateInstance(modalEl);

    // Clone the button to wipe all previous event listeners/onclick
    const oldBtn = document.getElementById('btn-confirm-reject');
    const newBtn = oldBtn.cloneNode(true);
    oldBtn.parentNode.replaceChild(newBtn, oldBtn);

    newBtn.onclick = async () => {
      const reason = document.getElementById('reject-reason-text').value.trim();
      if (!reason) { toast('Vui lòng nhập lý do từ chối.', 'error'); return; }
      try {
        await api('POST', `/overtime/admin/${id}/reject`, { reject_reason: reason });
        toast('Đã từ chối OT.', 'success');
        modal.hide();
        // Refresh table
        const now2 = new Date();
        const params = new URLSearchParams({ month: now2.getMonth() + 1, year: now2.getFullYear(), status: 'Pending' });
        const updated = await api('GET', `/overtime/admin/list?${params}`);
        const tbody = document.getElementById('dashboard-ot-pending-tbody');
        if (tbody) tbody.innerHTML = renderOTPendingSection(updated);
        // Update badge
        const badge = document.querySelector('#dashboard-ot-pending-table')?.closest('.glass-card')?.querySelector('.badge');
        if (badge) {
          if (updated.length > 0) { badge.textContent = updated.length; }
          else { badge.style.display = 'none'; }
        }
      } catch (e) { toast(e.message, 'error'); }
    };
    modal.show();
  };
}
