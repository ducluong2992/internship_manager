// ═══════════════════════════════════════════
// ADMIN SCHEDULE VIEW
// ═══════════════════════════════════════════
async function renderAdminSchedule(area) {
  const now = new Date();
  let selMonth = now.getMonth() + 1, selYear = now.getFullYear();

  async function load() {
    area.querySelector && area.querySelectorAll && null;
    const data = await api('GET', `/admin/schedule?month=${selMonth}&year=${selYear}`);
    renderGrid(data);
  }

  function renderGrid(data) {
    const workdays = getWorkdays(selYear, selMonth);
    const DOW = ['CN','T2','T3','T4','T5','T6','T7'];

    const headerCells = workdays.map(d => {
      const dow = new Date(selYear, selMonth-1, d).getDay();
      return `<div class="sched-head-cell">${d}<br><span style="font-size:0.65rem">${DOW[dow]}</span></div>`;
    }).join('');

    let rows = '';
    const registeredCount = data.rows ? data.rows.filter(r => r.schedules && r.schedules.length > 0).length : 0;

    if (!data.period) {
      rows = `<div style="grid-column:1/-1;text-align:center;padding:48px;color:var(--text-muted)">
        <i class="bi bi-calendar-x" style="font-size:2.5rem;display:block;margin-bottom:12px;opacity:0.4"></i>
        <strong>Chưa có kỳ đăng ký cho tháng ${selMonth}/${selYear}</strong><br>
        <small>Vào mục Kỳ đăng ký để tạo mới</small>
      </div>`;
    } else if (!data.rows || !data.rows.length) {
      rows = `<div style="grid-column:1/-1;text-align:center;padding:48px;color:var(--text-muted)">
        <i class="bi bi-inbox" style="font-size:2.5rem;display:block;margin-bottom:12px;opacity:0.4"></i>
        Chưa có thực tập sinh đăng ký
      </div>`;
    } else {
      // grand total row at bottom
      let grandTotal = 0;
      rows = data.rows.map(row => {
        const schedMap = {};
        (row.schedules || []).forEach(s => { schedMap[s.work_day] = s.shift; });
        let total = 0;
        const cells = workdays.map(d => {
          const dateStr = toDateStr(selYear, selMonth, d);
          const shift = schedMap[dateStr] || '';
          if (shift === 'SC') total += 1;
          else if (shift === 'S' || shift === 'C') total += 0.5;
          const cls = shift === 'SC' ? 'shift-sc' : (shift ? 'shift-s' : '');
          return `<div class="sched-cell"><span class="${cls}" style="padding:2px 6px;border-radius:4px;font-weight:${shift?700:400}">${shift||'<span style="opacity:.25">·</span>'}</span></div>`;
        }).join('');
        grandTotal += total;
        const hasSchedule = row.schedules && row.schedules.length > 0;
        return `
        <div class="sched-data-row" style="--day-count:${workdays.length}">
          <div class="sched-name-cell">
            <strong>${row.full_name}</strong><br>
            <small class="text-muted">${row.employee_code}${row.project ? ' · ' + row.project : ''}</small>
            ${!hasSchedule ? '<br><span style="font-size:0.68rem;color:var(--text-muted);opacity:.6">Chưa đăng ký</span>' : ''}
          </div>
          ${cells}
          <div class="sched-cell"><span class="total-badge">${total > 0 ? total : '—'}</span></div>
        </div>`;
      }).join('');

      // Grand total footer row
      rows += `
      <div class="sched-data-row" style="--day-count:${workdays.length};background:rgba(229,57,53,0.06);font-weight:700">
        <div class="sched-name-cell" style="color:var(--primary)">
          <i class="bi bi-sigma me-1"></i>TỔNG CỘNG
          <small style="display:block;color:var(--text-muted);font-weight:400">${registeredCount}/${data.rows.length} đã đăng ký</small>
        </div>
        ${workdays.map(() => '<div class="sched-cell"></div>').join('')}
        <div class="sched-cell"><span class="total-badge" style="background:rgba(229,57,53,.2);font-size:.9rem">${grandTotal}</span></div>
      </div>`;
    }

    area.innerHTML = `
<div class="section-header">
  <div class="section-title"><i class="bi bi-table text-warning"></i> Bảng lịch thực tập</div>
  <div class="d-flex gap-2 align-items-center flex-wrap">
    <select id="sel-month" class="form-select form-select-sm" style="width:130px">
      ${[...Array(12)].map((_,i)=>`<option value="${i+1}" ${i+1===selMonth?'selected':''}>Tháng ${i+1}</option>`).join('')}
    </select>
    <select id="sel-year" class="form-select form-select-sm" style="width:100px">
      ${[2024,2025,2026,2027].map(y=>`<option ${y===selYear?'selected':''}>${y}</option>`).join('')}
    </select>
    <button class="btn btn-sm btn-primary" id="btn-load-sched"><i class="bi bi-search me-1"></i>Xem</button>
    <button class="btn btn-sm btn-outline-success" id="btn-export">
      <i class="bi bi-file-earmark-excel-fill me-1"></i>Xuất Excel
    </button>
  </div>
</div>

${data.period ? `
<div class="d-flex gap-3 mb-3 align-items-center flex-wrap">
  <span>${badgePeriod(data.period.status)}</span>
  <span class="small text-muted">Mở: ${fmtDateTime(data.period.open_date)}</span>
  <span class="small text-muted">Đóng: ${fmtDateTime(data.period.close_date)}</span>
  <span class="small text-muted">Đăng ký: <strong class="text-info">${registeredCount}/${data.rows.length}</strong> thực tập sinh</span>
</div>` : ''}

<div class="schedule-grid-wrap">
  <div class="schedule-grid" style="--day-count:${workdays.length}">
    <div class="sched-header-row" style="--day-count:${workdays.length}">
      <div class="sched-head-name">Họ tên / Mã NV</div>
      ${headerCells}
      <div class="sched-head-cell">Tổng</div>
    </div>
    ${rows}
  </div>
</div>`;

    document.getElementById('btn-load-sched').onclick = async () => {
      selMonth = parseInt(document.getElementById('sel-month').value);
      selYear = parseInt(document.getElementById('sel-year').value);
      area.innerHTML = '<div class="d-flex justify-content-center py-5"><div class="spinner-border text-danger"></div></div>';
      await load();
    };

    document.getElementById('btn-export').onclick = async () => {
      try {
        const res = await fetch(`${API}/admin/schedule/export?month=${selMonth}&year=${selYear}`, {
          headers: { Authorization: 'Bearer ' + STATE.token }
        });
        if (!res.ok) { toast('Lỗi xuất file', 'error'); return; }
        const blob = await res.blob();
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url; a.download = `lich_t${selMonth}_${selYear}.xlsx`;
        document.body.appendChild(a); a.click();
        document.body.removeChild(a); URL.revokeObjectURL(url);
        toast('Xuất Excel thành công', 'success');
      } catch { toast('Lỗi xuất file', 'error'); }
    };
  }

  await load();
}
