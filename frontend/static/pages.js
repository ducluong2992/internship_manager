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
  if (s === 'Working') return `<span class="custom-badge badge-working"><i class="bi bi-circle-fill" style="font-size:7px"></i> Đang làm</span>`;
  return `<span class="custom-badge badge-resigned"><i class="bi bi-circle-fill" style="font-size:7px"></i> Đã nghỉ</span>`;
}
function badgePeriod(s) {
  return s === 'open'
    ? `<span class="custom-badge badge-open"><i class="bi bi-unlock-fill" style="font-size:9px"></i> Mở</span>`
    : `<span class="custom-badge badge-closed"><i class="bi bi-lock-fill" style="font-size:9px"></i> Đóng</span>`;
}

// ═══════════════════════════════════════════
// RENDER DISPATCHER
// ═══════════════════════════════════════════
async function renderPage(page, area) {
  try {
    switch (page) {
      case 'dashboard':         await renderDashboard(area); break;
      case 'manage-users':      await renderManageUsers(area); break;
      case 'manage-periods':    await renderManagePeriods(area); break;
      case 'manage-accounts':   await renderManageAccounts(area); break;
      case 'admin-schedule':    await renderAdminSchedule(area); break;
      case 'profile':           await renderProfile(area); break;
      case 'register-schedule': await renderRegisterSchedule(area); break;
      case 'view-schedule':     await renderViewSchedule(area); break;
      case 'change-password':   renderChangePassword(area); break;
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
  const interns = users.filter(u => u.role === 'intern');
  const now = new Date();
  const thisPeriod = periods.find(p => p.month === now.getMonth()+1 && p.year === now.getFullYear());

  area.innerHTML = `
<div class="row g-4 mb-4">
  <div class="col-6 col-lg-3">
    <div class="stat-card">
      <div class="stat-icon red"><i class="bi bi-people-fill"></i></div>
      <div><div class="stat-value">${stats.total_interns}</div><div class="stat-label">Tổng thực tập sinh</div></div>
    </div>
  </div>
  <div class="col-6 col-lg-3">
    <div class="stat-card">
      <div class="stat-icon green"><i class="bi bi-person-check-fill"></i></div>
      <div><div class="stat-value">${stats.working}</div><div class="stat-label">Đang làm việc</div></div>
    </div>
  </div>
  <div class="col-6 col-lg-3">
    <div class="stat-card">
      <div class="stat-icon blue"><i class="bi bi-person-badge-fill"></i></div>
      <div><div class="stat-value">${stats.intern_count}</div><div class="stat-label">Thực tập</div></div>
    </div>
  </div>
  <div class="col-6 col-lg-3">
    <div class="stat-card">
      <div class="stat-icon yellow"><i class="bi bi-arrow-left-right"></i></div>
      <div><div class="stat-value">${stats.borrowed_count}</div><div class="stat-label">Đi mượn</div></div>
    </div>
  </div>
</div>

<div class="row g-4">
  <div class="col-lg-8">
    <div class="glass-card p-4">
      <div class="section-header">
        <div class="section-title"><i class="bi bi-people-fill text-danger"></i> Thực tập sinh gần đây</div>
        <button class="btn btn-sm btn-primary" onclick="navigate('manage-users')"><i class="bi bi-arrow-right me-1"></i>Xem tất cả</button>
      </div>
      <div class="table-responsive">
        <table class="table table-hover">
          <thead><tr><th>Mã NV</th><th>Họ tên</th><th>Dự án</th><th>Loại nhân sự</th><th>Trạng thái</th></tr></thead>
          <tbody>
            ${interns.slice(0,8).map(u=>`
            <tr>
              <td><code>${u.employee_code}</code></td>
              <td><strong>${u.full_name}</strong></td>
              <td>${u.project||'—'}</td>
              <td><span class="badge ${(u.employee_type||'').toLowerCase()==='borrowed' ? 'bg-warning text-dark' : 'bg-info text-dark'}">${(u.employee_type||'').toLowerCase()==='borrowed' ? 'Đi mượn' : 'Thực tập'}</span></td>
              <td>${badgeStatus(u.working_status)}</td>
            </tr>`).join('')}
          </tbody>
        </table>
      </div>
    </div>
  </div>
  <div class="col-lg-4">
    <div class="glass-card p-4 mb-4">
      <div class="section-title mb-3"><i class="bi bi-pie-chart-fill text-info"></i> Phân bổ nhân sự</div>
      <div class="d-flex flex-column gap-2">
        <div class="d-flex justify-content-between align-items-center">
          <span class="small text-muted">Thực tập</span>
          <div class="d-flex align-items-center gap-2">
            <div style="width:100px;height:8px;background:var(--border);border-radius:4px;overflow:hidden">
              <div style="width:${stats.total_interns?Math.round(stats.intern_count/stats.total_interns*100):0}%;height:100%;background:var(--info);border-radius:4px"></div>
            </div>
            <strong class="small">${stats.intern_count}</strong>
          </div>
        </div>
        <div class="d-flex justify-content-between align-items-center">
          <span class="small text-muted">Đi mượn</span>
          <div class="d-flex align-items-center gap-2">
            <div style="width:100px;height:8px;background:var(--border);border-radius:4px;overflow:hidden">
              <div style="width:${stats.total_interns?Math.round(stats.borrowed_count/stats.total_interns*100):0}%;height:100%;background:var(--warning);border-radius:4px"></div>
            </div>
            <strong class="small">${stats.borrowed_count}</strong>
          </div>
        </div>
        <div class="d-flex justify-content-between align-items-center">
          <span class="small text-muted">Đã nghỉ</span>
          <div class="d-flex align-items-center gap-2">
            <div style="width:100px;height:8px;background:var(--border);border-radius:4px;overflow:hidden">
              <div style="width:${stats.total_interns?Math.round(stats.resigned/stats.total_interns*100):0}%;height:100%;background:var(--danger);border-radius:4px"></div>
            </div>
            <strong class="small">${stats.resigned}</strong>
          </div>
        </div>
      </div>
    </div>
    <div class="glass-card p-4">
      <div class="section-title mb-3"><i class="bi bi-calendar-check text-success"></i> Kỳ tháng ${now.getMonth()+1}/${now.getFullYear()}</div>
      ${thisPeriod ? `
        <div class="mb-3">${badgePeriod(thisPeriod.status)}</div>
        <div class="mb-2 small text-muted"><i class="bi bi-calendar-event me-2"></i>Mở: ${fmtDateTime(thisPeriod.open_date)}</div>
        <div class="mb-3 small text-muted"><i class="bi bi-calendar-x me-2"></i>Đóng: ${fmtDateTime(thisPeriod.close_date)}</div>
        <button class="btn btn-sm btn-primary w-100" onclick="navigate('admin-schedule')">
          <i class="bi bi-table me-1"></i>Xem bảng lịch
        </button>
      ` : `
        <div class="empty-state py-3">
          <i class="bi bi-calendar-plus" style="font-size:2rem"></i>
          <p>Chưa có kỳ đăng ký</p>
          <button class="btn btn-sm btn-primary mt-2" onclick="navigate('manage-periods')">Tạo ngay</button>
        </div>
      `}
    </div>
  </div>
</div>`;
}
