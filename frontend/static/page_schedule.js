// ═══════════════════════════════════════════
// REGISTER SCHEDULE (Intern)
// ═══════════════════════════════════════════
async function renderRegisterSchedule(area) {
  const periods = await api('GET', '/schedule/periods');
  const openPeriods = periods.filter(p => p.status === 'open');

  if (!openPeriods.length) {
    area.innerHTML = `
<div class="glass-card p-5 text-center">
  <i class="bi bi-calendar-x" style="font-size:3.5rem;color:var(--text-muted);display:block;margin-bottom:16px;opacity:.5"></i>
  <h5 class="fw-bold mb-2">Hiện chưa có kỳ đăng ký nào đang mở</h5>
  <p class="text-muted small mb-4">Vui lòng liên hệ Admin để được hỗ trợ</p>
  <button class="btn btn-outline-secondary btn-sm" onclick="navigate('view-schedule')">
    <i class="bi bi-calendar-week me-1"></i>Xem lịch đã đăng ký
  </button>
</div>`;
    return;
  }

  let selPeriodId = openPeriods[0].id;
  let shiftMap = {};
  let isSaving = false;

  async function loadExisting(periodId) {
    const list = await api('GET', `/schedule/me?period_id=${periodId}`);
    shiftMap = {};
    list.forEach(s => { shiftMap[s.work_day] = s.shift || ''; });
  }

  function cycleShift(current) {
    const order = ['', 'S', 'C', 'SC'];
    return order[(order.indexOf(current) + 1) % order.length];
  }

  function calcTotal() {
    return Object.values(shiftMap).reduce((acc, s) => {
      if (s === 'SC') return acc + 1;
      if (s === 'S' || s === 'C') return acc + 0.5;
      return acc;
    }, 0);
  }

  function isClosingSoon(period) {
    if (!period.close_date) return false;
    const diff = new Date(period.close_date) - new Date();
    return diff > 0 && diff < 24 * 60 * 60 * 1000; // < 24h
  }

  async function renderCalendar() {
    const period = periods.find(p => p.id === selPeriodId);
    const y = period.year, m = period.month;
    const totalDays = daysInMonth(y, m);
    const firstDow = new Date(y, m - 1, 1).getDay();
    const adjFirst = (firstDow + 6) % 7; // Mon=0

    let calCells = '';
    for (let i = 0; i < adjFirst; i++) calCells += '<div class="cal-day cal-empty"></div>';

    for (let d = 1; d <= totalDays; d++) {
      const dow = new Date(y, m - 1, d).getDay();
      const isWeekend = dow === 0 || dow === 6;
      const dateStr = toDateStr(y, m, d);
      const shift = shiftMap[dateStr] || '';
      const shiftCls = shift ? `cal-shift-${shift}` : 'cal-shift-none';
      const shiftLabel = shift || '—';

      calCells += `<div class="cal-day ${isWeekend ? 'cal-weekend' : 'reg-cell'}"
        data-date="${dateStr}" ${isWeekend ? '' : 'onclick="cycleDay(this)"'}>
        <div class="cal-day-num">${d}</div>
        <div class="cal-shift-badge ${shiftCls}" id="badge-${dateStr}">${shiftLabel}</div>
      </div>`;
    }

    const warnHtml = isClosingSoon(period)
      ? `<div class="alert alert-danger py-2 small mb-3"><i class="bi bi-exclamation-triangle-fill me-2"></i>
         Kỳ đăng ký sắp đóng! Hạn: ${new Date(period.close_date).toLocaleString('vi-VN',{hour12:false})}</div>`
      : '';

    area.innerHTML = `
<div class="section-header">
  <div class="section-title"><i class="bi bi-calendar-check-fill text-success"></i> Đăng ký lịch thực tập</div>
  <button class="btn btn-primary btn-sm" id="btn-submit-sched">
    <i class="bi bi-send-fill me-1"></i>Lưu lịch
  </button>
</div>

${warnHtml}

<div class="glass-card p-3 mb-3 d-flex gap-3 align-items-center flex-wrap">
  <label class="fw-semibold small text-muted mb-0">Chọn kỳ:</label>
  <select id="period-select" class="form-select form-select-sm" style="max-width:200px">
    ${openPeriods.map(p=>`<option value="${p.id}" ${p.id===selPeriodId?'selected':''}>Tháng ${p.month}/${p.year}</option>`).join('')}
  </select>
  <div class="ms-auto d-flex align-items-center gap-2">
    <span class="badge bg-success">Tổng: <span id="total-sessions">${calcTotal()}</span> buổi</span>
    <button class="btn btn-outline-secondary btn-sm" id="btn-clear-sched" title="Xóa hết">
      <i class="bi bi-eraser-fill"></i>
    </button>
  </div>
</div>

<div class="glass-card p-4 mb-3">
  <div class="d-flex gap-3 mb-3 flex-wrap align-items-center">
    <span class="small fw-semibold text-muted">Nhấp vào ngày để chọn ca:</span>
    <span class="cal-shift-badge cal-shift-none px-3">— Trống</span>
    <span class="cal-shift-badge cal-shift-S px-3">S = Sáng (0.5)</span>
    <span class="cal-shift-badge cal-shift-C px-3">C = Chiều (0.5)</span>
    <span class="cal-shift-badge cal-shift-SC px-3">SC = Cả ngày (1)</span>
  </div>
  <div class="cal-grid">
    <div class="cal-day-head">T2</div><div class="cal-day-head">T3</div>
    <div class="cal-day-head">T4</div><div class="cal-day-head">T5</div>
    <div class="cal-day-head">T6</div>
    <div class="cal-day-head" style="color:var(--warning)">T7</div>
    <div class="cal-day-head" style="color:var(--danger)">CN</div>
    ${calCells}
  </div>
</div>`;

    document.getElementById('period-select').onchange = async (e) => {
      selPeriodId = parseInt(e.target.value);
      await loadExisting(selPeriodId);
      await renderCalendar();
    };

    document.getElementById('btn-clear-sched').onclick = () => {
      if (!confirm('Xóa toàn bộ ca đã chọn?')) return;
      shiftMap = {};
      renderCalendar();
    };

    document.getElementById('btn-submit-sched').onclick = async () => {
      if (isSaving) return;
      isSaving = true;
      const btn = document.getElementById('btn-submit-sched');
      btn.disabled = true;
      btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Đang lưu...';
      const entries = Object.entries(shiftMap)
        .filter(([, s]) => s && ['S','C','SC'].includes(s))
        .map(([work_day, shift]) => ({ work_day, shift }));
      try {
        await api('POST', '/schedule/me', { period_id: selPeriodId, entries });
        toast(`Đã lưu lịch — Tổng ${calcTotal()} buổi`, 'success');
      } catch (e) { toast(e.message, 'error'); }
      finally {
        isSaving = false;
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-send-fill me-1"></i>Lưu lịch';
      }
    };
  }

  window.cycleDay = function(el) {
    const dateStr = el.dataset.date;
    const current = shiftMap[dateStr] || '';
    const next = cycleShift(current);
    shiftMap[dateStr] = next;
    const badge = document.getElementById('badge-' + dateStr);
    badge.className = `cal-shift-badge cal-shift-${next || 'none'}`;
    badge.textContent = next || '—';
    document.getElementById('total-sessions').textContent = calcTotal();
  };

  await loadExisting(selPeriodId);
  await renderCalendar();
}

// ═══════════════════════════════════════════
// VIEW MY SCHEDULE (Intern)
// ═══════════════════════════════════════════
async function renderViewSchedule(area) {
  const periods = await api('GET', '/schedule/periods');

  if (!periods.length) {
    area.innerHTML = `<div class="empty-state glass-card p-5">
      <i class="bi bi-calendar-week-fill"></i><p>Chưa có kỳ đăng ký nào</p></div>`;
    return;
  }

  const now = new Date();
  let selPeriod = periods.find(p => p.month === now.getMonth()+1 && p.year === now.getFullYear()) || periods[0];

  async function load() {
    const schedules = await api('GET', `/schedule/me?period_id=${selPeriod.id}`);
    const workdays = getWorkdays(selPeriod.year, selPeriod.month);
    const DOW = ['CN','T2','T3','T4','T5','T6','T7'];
    const schedMap = {};
    schedules.forEach(s => { schedMap[s.work_day] = s.shift; });
    let total = 0;

    const rows = workdays.map(d => {
      const dateStr = toDateStr(selPeriod.year, selPeriod.month, d);
      const shift = schedMap[dateStr] || '';
      if (shift === 'SC') total += 1;
      else if (shift === 'S' || shift === 'C') total += 0.5;
      const dow = new Date(selPeriod.year, selPeriod.month-1, d).getDay();
      const shiftCls = shift === 'SC' ? 'shift-sc' : (shift ? 'shift-s' : '');
      return `<tr>
        <td class="text-center">${d}</td>
        <td class="text-center">${DOW[dow]}</td>
        <td class="text-center">
          ${shift ? `<span class="${shiftCls}" style="padding:3px 14px;border-radius:6px;font-weight:700">${shift}</span>`
                  : '<span class="text-muted">—</span>'}
        </td>
        <td class="text-center text-muted">${shift === 'SC' ? '1' : shift ? '0.5' : '0'}</td>
      </tr>`;
    }).join('');

    area.innerHTML = `
<div class="section-header">
  <div class="section-title"><i class="bi bi-calendar-week-fill text-primary"></i> Lịch của tôi</div>
  <div class="d-flex gap-2 align-items-center flex-wrap">
    <select id="view-period-sel" class="form-select form-select-sm" style="width:220px">
      ${periods.map(p=>`<option value="${p.id}" ${p.id===selPeriod.id?'selected':''}>Tháng ${p.month}/${p.year} ${p.status==='open'?'🟢':''}</option>`).join('')}
    </select>
    ${selPeriod.status === 'open' ? `<button class="btn btn-sm btn-primary" onclick="navigate('register-schedule')">
      <i class="bi bi-pencil-fill me-1"></i>Chỉnh sửa</button>` : ''}
  </div>
</div>

<div class="row g-4">
  <div class="col-lg-4">
    <div class="glass-card p-4 text-center">
      <div class="stat-value" style="font-size:3.5rem;color:var(--primary)">${total}</div>
      <div class="stat-label mb-2">Tổng buổi đăng ký</div>
      <div class="small text-muted mb-3">Tháng ${selPeriod.month}/${selPeriod.year}</div>
      <div>${badgePeriod(selPeriod.status)}</div>
      ${selPeriod.close_date ? `<div class="mt-2 small text-muted">Hạn đăng ký: ${new Date(selPeriod.close_date).toLocaleString('vi-VN',{hour12:false})}</div>` : ''}
    </div>
  </div>
  <div class="col-lg-8">
    <div class="table-wrap">
      <div class="table-responsive">
        <table class="table">
          <thead><tr><th class="text-center">Ngày</th><th class="text-center">Thứ</th><th class="text-center">Ca</th><th class="text-center">Số buổi</th></tr></thead>
          <tbody>${rows || '<tr><td colspan="4" class="text-center py-4 text-muted">Chưa đăng ký lịch</td></tr>'}</tbody>
          <tfoot><tr>
            <td colspan="3" class="text-end fw-bold">Tổng cộng</td>
            <td class="text-center fw-bold text-danger">${total}</td>
          </tr></tfoot>
        </table>
      </div>
    </div>
  </div>
</div>`;

    document.getElementById('view-period-sel').onchange = async (e) => {
      selPeriod = periods.find(p => p.id === parseInt(e.target.value));
      await load();
    };
  }

  await load();
}

// ═══════════════════════════════════════════
// CHANGE PASSWORD
// ═══════════════════════════════════════════
function renderChangePassword(area) {
  area.innerHTML = `
<div class="row justify-content-center">
  <div class="col-md-5">
    <div class="glass-card p-4">
      <div class="section-title mb-4"><i class="bi bi-key-fill text-warning"></i> Đổi mật khẩu</div>
      <form id="form-change-pwd" autocomplete="off">
        <div class="mb-3">
          <label class="form-label">Mật khẩu hiện tại</label>
          <div class="input-icon-wrap">
            <i class="bi bi-lock-fill input-icon"></i>
            <input id="cp-old" type="password" class="form-control input-with-icon" required />
            <button type="button" class="btn-eye" onclick="togglePass('cp-old',this)"><i class="bi bi-eye"></i></button>
          </div>
        </div>
        <div class="mb-3">
          <label class="form-label">Mật khẩu mới <small class="text-muted">(tối thiểu 6 ký tự)</small></label>
          <div class="input-icon-wrap">
            <i class="bi bi-shield-lock-fill input-icon"></i>
            <input id="cp-new" type="password" class="form-control input-with-icon" required minlength="6" />
            <button type="button" class="btn-eye" onclick="togglePass('cp-new',this)"><i class="bi bi-eye"></i></button>
          </div>
        </div>
        <div class="mb-4">
          <label class="form-label">Xác nhận mật khẩu mới</label>
          <div class="input-icon-wrap">
            <i class="bi bi-shield-check-fill input-icon"></i>
            <input id="cp-confirm" type="password" class="form-control input-with-icon" required />
            <button type="button" class="btn-eye" onclick="togglePass('cp-confirm',this)"><i class="bi bi-eye"></i></button>
          </div>
        </div>
        <div id="cp-err" class="alert alert-danger d-none mb-3"></div>
        <button type="submit" class="btn btn-primary w-100" id="btn-change-pwd">
          <i class="bi bi-check-circle-fill me-2"></i>Đổi mật khẩu
        </button>
      </form>
    </div>
  </div>
</div>`;

  document.getElementById('form-change-pwd').onsubmit = async (e) => {
    e.preventDefault();
    const errEl = document.getElementById('cp-err');
    errEl.classList.add('d-none');
    const btn = document.getElementById('btn-change-pwd');
    const oldP = document.getElementById('cp-old').value;
    const newP = document.getElementById('cp-new').value;
    const conf = document.getElementById('cp-confirm').value;
    if (newP !== conf) {
      errEl.textContent = 'Mật khẩu xác nhận không khớp';
      errEl.classList.remove('d-none'); return;
    }
    btn.disabled = true; btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Đang xử lý...';
    try {
      const r = await api('POST', '/auth/change-password', { old_password: oldP, new_password: newP });
      toast(r.message, 'success');
      e.target.reset();
    } catch (err) {
      errEl.textContent = err.message;
      errEl.classList.remove('d-none');
    } finally {
      btn.disabled = false; btn.innerHTML = '<i class="bi bi-check-circle-fill me-2"></i>Đổi mật khẩu';
    }
  };
}
