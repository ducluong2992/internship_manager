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
      case 'manage-accounts': await renderManageAccounts(area, 'intern'); break;
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
  const now = new Date();

  // Calculations for progress bars
  const totalAll = (stats.total_employees + stats.total_interns) || users.length || 1;
  const inProjectCount = users.filter(u => u.project && u.project.trim() !== '' && u.project !== '—').length;
  const noProjectCount = Math.max(0, totalAll - inProjectCount);

  const empTT = stats.emp_trung_tam || 0;
  const internTT = stats.intern_count || 0;
  const empOnsite = stats.emp_onsite || 0;
  const empBorrow = stats.emp_cho_muon || 0;
  const internBorrow = stats.borrowed_count || 0;

  const totalTT = empTT + internTT;
  const totalBorrow = empBorrow + internBorrow;

  const pctEmp = Math.round((stats.total_employees / totalAll) * 100);
  const pctIntern = Math.round((stats.total_interns / totalAll) * 100);

  const pctInProj = Math.round((inProjectCount / totalAll) * 100);
  const pctNoProj = Math.round((noProjectCount / totalAll) * 100);

  const pctTT = Math.round((totalTT / totalAll) * 100);
  const pctOnsite = Math.round((empOnsite / totalAll) * 100);
  const pctBorrow = Math.round((totalBorrow / totalAll) * 100);

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
        <span class="stat-number">${stats.working}</span>
        <span class="stat-unit">nhân sự</span>
      </div>
    </div>
  </div>
  <div class="col-12 col-sm-6 col-xl-3">
    <div class="viettel-stat-card card-red-2 shadow-sm">
      <div class="stat-title">ĐANG HỌC VIỆC</div>
      <div class="d-flex align-items-baseline">
        <span class="stat-number">${stats.intern_count}</span>
        <span class="stat-unit">nhân sự</span>
      </div>
    </div>
  </div>
  <div class="col-12 col-sm-6 col-xl-3">
    <div class="viettel-stat-card card-red-3 shadow-sm">
      <div class="stat-title">ĐANG VÀO DỰ ÁN</div>
      <div class="d-flex align-items-baseline">
        <span class="stat-number">${stats.emp_onsite}</span>
        <span class="stat-unit">nhân sự</span>
      </div>
    </div>
  </div>
  <div class="col-12 col-sm-6 col-xl-3">
    <div class="viettel-stat-card card-red-4 shadow-sm">
      <div class="stat-title">SẮP HẾT HẠN / CHUẨN BỊ OUT</div>
      <div class="d-flex align-items-baseline">
        <span class="stat-number">${stats.resigned}</span>
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
        ${renderStatBar('Nhân viên', stats.total_employees, totalAll, pctEmp, ['#3B82F6', '#1D4ED8'])}
        ${renderStatBar('Thực tập sinh', stats.total_interns, totalAll, pctIntern, ['#06B6D4', '#0284C7'])}
      </div>
    </div>
  </div>
  <div class="col-md-4">
    <div class="glass-card p-4 h-100 d-flex flex-column">
      <h6 class="fw-bold mb-3"><i class="bi bi-briefcase-fill text-danger me-2"></i>Nhân sự trong Dự án / Không trong Dự án</h6>
      <div class="flex-grow-1 d-flex flex-column justify-content-center pt-2">
        ${renderStatBar('Đang trong dự án', inProjectCount, totalAll, pctInProj, ['#10B981', '#047857'])}
        ${renderStatBar('Không trong dự án', noProjectCount, totalAll, pctNoProj, ['#F59E0B', '#B45309'])}
      </div>
    </div>
  </div>
  <div class="col-md-4">
    <div class="glass-card p-4 h-100 d-flex flex-column">
      <h6 class="fw-bold mb-3"><i class="bi bi-diagram-3-fill text-danger me-2"></i>Nhân sự theo Loại Nhân sự</h6>
      <div class="flex-grow-1 d-flex flex-column justify-content-center pt-2">
        ${renderStatBar('NS Trung tâm', totalTT, totalAll, pctTT, ['#8B5CF6', '#6D28D9'])}
        ${renderStatBar('Onsite / Dự án', empOnsite, totalAll, pctOnsite, ['#EC4899', '#BE185D'])}
        ${renderStatBar('Cho mượn / Đi mượn', totalBorrow, totalAll, pctBorrow, ['#F97316', '#C2410C'])}
      </div>
    </div>
  </div>
</div>

<!-- 3. TODAY WORKERS -->
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
</div>`;
}
