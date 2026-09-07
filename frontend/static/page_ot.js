/* ══════════════════════════════════════════════════════════════════════════
   PAGE_OT.JS — Chấm công OT (nhân sự) + Quản lý OT (Admin)
   ══════════════════════════════════════════════════════════════════════════ */

// ─── Helpers ─────────────────────────────────────────────────────────────────
function otBadge(status) {
  if (status === 'Approved') return `<span class="custom-badge" style="background:rgba(46,125,50,.15);color:#2e7d32;border:1px solid rgba(46,125,50,.3)">Đã duyệt</span>`;
  if (status === 'Pending') return `<span class="custom-badge" style="background:rgba(237,108,2,.15);color:#e65100;border:1px solid rgba(237,108,2,.3)">Chờ duyệt</span>`;
  return `<span class="custom-badge" style="background:rgba(211,47,47,.15);color:#d32f2f;border:1px solid rgba(211,47,47,.3)">Từ chối</span>`;
}

function otCalendarDotClass(status) {
  if (status === 'Approved') return 'ot-dot-approved';
  if (status === 'Pending') return 'ot-dot-pending';
  return 'ot-dot-rejected';
}

function formatTime24h(timeStr) {
  if (!timeStr) return '';
  let str = timeStr.trim().toUpperCase();
  if (str.includes('AM') || str.includes('PM')) {
    const isPM = str.includes('PM');
    str = str.replace('AM', '').replace('PM', '').trim();
    let [h, m] = str.split(':').map(Number);
    if (isPM && h < 12) h += 12;
    if (!isPM && h === 12) h = 0;
    return `${String(h).padStart(2, '0')}:${String(m || 0).padStart(2, '0')}`;
  }
  const parts = str.split(':');
  if (parts.length >= 2) {
    const h = String(parseInt(parts[0])).padStart(2, '0');
    const m = String(parseInt(parts[1])).padStart(2, '0');
    return `${h}:${m}`;
  }
  return timeStr;
}

// ══════════════════════════════════════════════════════════════════════════════
// NHÂN SỰ — Đăng ký / Xem OT
// ══════════════════════════════════════════════════════════════════════════════
async function renderRegisterOT(area) {
  const now = new Date();
  let curMonth = now.getMonth() + 1;
  let curYear = now.getFullYear();

  // Kiểm tra quyền Onsite
  const stats = await api('GET', `/overtime/my/stats?month=${curMonth}&year=${curYear}`);
  if (!stats.is_onsite || !stats.has_project) {
    area.innerHTML = `
    <div class="d-flex justify-content-center align-items-center" style="min-height:60vh">
      <div class="glass-card p-5 text-center" style="max-width:480px">
        <div style="font-size:3rem;margin-bottom:1rem">🔒</div>
        <h5 class="fw-bold mb-2">Chưa được cấp quyền đăng ký OT</h5>
        <p class="text-muted mb-0">Bạn chưa được cấp quyền đăng ký OT. Vui lòng liên hệ quản trị viên.</p>
      </div>
    </div>`;
    return;
  }

  function renderOTPage(month, year, records, st) {
    const daysInM = new Date(year, month, 0).getDate();
    const firstDow = new Date(year, month - 1, 1).getDay(); // 0=CN
    // Tạo map ngày → danh sách OT
    const dayMap = {};
    records.forEach(r => {
      const d = parseInt(r.work_date.split('-')[2]);
      if (!dayMap[d]) dayMap[d] = [];
      dayMap[d].push(r);
    });

    // Calendar grid
    const todayObj = new Date();
    const todayStr = `${todayObj.getFullYear()}-${String(todayObj.getMonth() + 1).padStart(2, '0')}-${String(todayObj.getDate()).padStart(2, '0')}`;

    let calCells = '';
    const startOffset = (firstDow === 0 ? 6 : firstDow - 1); // T2=0, CN=6
    for (let i = 0; i < startOffset; i++) calCells += `<div class="ot-cal-cell ot-cal-empty"></div>`;
    for (let d = 1; d <= daysInM; d++) {
      const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
      const dow = new Date(year, month - 1, d).getDay();
      const isWeekend = dow === 0 || dow === 6;
      const isToday = (dateStr === todayStr);
      const dayRecs = dayMap[d] || [];
      const dots = dayRecs.map(r => `<span class="ot-cal-dot ${otCalendarDotClass(r.status)}"></span>`).join('');
      const dayTotalRaw = dayRecs.reduce((acc, r) => acc + r.raw_hours, 0);
      const hoursCenter = dayTotalRaw > 0 ? `<div class="ot-cal-center-hours">${dayTotalRaw}h</div>` : '';

      const hasOT = dayRecs.length > 0;
      const clickAction = hasOT ? `openOTDayHistoryModal('${dateStr}')` : `openOTRegisterModal('${dateStr}')`;
      const cellTitle = hasOT ? `Bấm để xem lịch sử OT ngày ${d}/${month}/${year}` : `Bấm để đăng ký OT ngày ${d}/${month}/${year}`;
      const todayClass = isToday ? 'ot-cal-today' : '';
      const todayBadge = isToday ? `<span class="badge bg-danger ms-1" style="font-size:0.65rem">Hôm nay</span>` : '';

      calCells += `
      <div class="ot-cal-cell ${isWeekend ? 'ot-cal-weekend' : ''} ${todayClass}" data-date="${dateStr}" onclick="${clickAction}" style="cursor:pointer" title="${cellTitle}">
        <div class="ot-cal-day d-flex align-items-center justify-content-between">
          <span>${d}</span>
          ${todayBadge}
        </div>
        ${hoursCenter}
        <div class="ot-cal-dots">${dots}</div>
      </div>`;
    }

    const DOW_NAMES = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];

    // Danh sách bên dưới
    let listRows = '';
    if (records.length === 0) {
      listRows = `<tr><td colspan="6" class="text-center text-muted py-4">Chưa có đăng ký OT nào trong tháng này.</td></tr>`;
    } else {
      records.forEach(r => {
        const parts = r.work_date.split('-');
        const dObj = new Date(parseInt(parts[0]), parseInt(parts[1]) - 1, parseInt(parts[2]));
        const dowStr = DOW_NAMES[dObj.getDay()];
        const formattedDate = `${dowStr}, ${parts[2]}/${parts[1]}/${parts[0]}`;

        const rejectInfo = r.status === 'Rejected' ? `
          <div class="mt-1 small text-danger"><i class="bi bi-chat-left-text me-1"></i>
            <strong>Lý do từ chối:</strong> ${r.reject_reason || ''}
          </div>
          <div class="mt-1 d-flex gap-1">
            <button class="btn btn-sm btn-outline-warning py-0" onclick="openOTEditModal(${r.id})"><i class="bi bi-pencil"></i> Sửa</button>
            <button class="btn btn-sm btn-outline-danger py-0" onclick="deleteOT(${r.id})"><i class="bi bi-trash"></i> Xóa</button>
          </div>` : '';
        const pendingActions = r.status === 'Pending' ? `
          <div class="mt-1 d-flex gap-1">
            <button class="btn btn-sm btn-outline-secondary py-0" onclick="openOTEditModal(${r.id})"><i class="bi bi-pencil"></i> Sửa</button>
            <button class="btn btn-sm btn-outline-danger py-0" onclick="deleteOT(${r.id})"><i class="bi bi-trash"></i> Xóa</button>
          </div>` : '';
        listRows += `
        <tr>
          <td class="fw-semibold">${formattedDate}</td>
          <td>${formatTime24h(r.start_time)} – ${formatTime24h(r.end_time)}</td>
          <td class="text-center">${r.raw_hours}h</td>
          <td class="text-center"><span class="badge bg-secondary">${r.factor}x</span></td>
          <td>${otBadge(r.status)}${rejectInfo}${pendingActions}</td>
          <td class="text-center fw-bold text-danger">${r.weighted_hours}h</td>
        </tr>`;
      });
    }

    area.innerHTML = `
    <div class="section-header mb-3">
      <div class="section-title"><i class="bi bi-clock-history text-danger"></i> Chấm công OT</div>
      <button class="btn btn-danger btn-sm px-3 fw-bold" onclick="openOTRegisterModal()"><i class="bi bi-plus-lg me-1"></i>Đăng ký OT</button>
    </div>

    <!-- Thống kê tháng -->
    <div class="row g-3 mb-4">
      <div class="col-md-4">
        <div class="glass-card p-3 text-center border-start border-danger border-4">
          <div class="text-muted small fw-semibold text-uppercase mb-1">Tháng ${String(month).padStart(2, '0')}/${year}</div>
          <div class="fs-2 fw-bold text-danger">${st.total_raw_hours}<span class="fs-6 text-muted fw-normal"> giờ OT</span></div>
          <div class="small text-muted mt-1">Giờ quy đổi: <strong>${st.total_weighted_hours}h</strong></div>
        </div>
      </div>
      <div class="col-md-2">
        <div class="glass-card p-3 text-center h-100 d-flex flex-column justify-content-center">
          <div class="text-warning fw-bold fs-4">${st.pending_count}</div>
          <div class="small text-muted">Chờ duyệt</div>
        </div>
      </div>
      <div class="col-md-2">
        <div class="glass-card p-3 text-center h-100 d-flex flex-column justify-content-center">
          <div class="text-success fw-bold fs-4">${st.approved_count}</div>
          <div class="small text-muted">Đã duyệt</div>
        </div>
      </div>
      <div class="col-md-2">
        <div class="glass-card p-3 text-center h-100 d-flex flex-column justify-content-center">
          <div class="text-danger fw-bold fs-4">${st.rejected_count}</div>
          <div class="small text-muted">Từ chối</div>
        </div>
      </div>
      <div class="col-md-2">
        <div class="glass-card p-3 text-center h-100 d-flex flex-column justify-content-center align-items-center">
          <div class="d-flex gap-2 small">
            <span class="ot-cal-dot ot-dot-approved"></span> Duyệt
            <span class="ot-cal-dot ot-dot-pending"></span> Chờ
            <span class="ot-cal-dot ot-dot-rejected"></span> Từ chối
          </div>
        </div>
      </div>
    </div>

    <!-- Calendar navigation -->
    <div class="glass-card p-4 mb-4">
      <div class="d-flex justify-content-between align-items-center mb-3">
        <button class="btn btn-outline-secondary btn-sm" id="ot-prev-month">
          <i class="bi bi-chevron-left"></i>
        </button>
        <h6 class="fw-bold mb-0">Tháng ${String(month).padStart(2, '0')}/${year}</h6>
        <button class="btn btn-outline-secondary btn-sm" id="ot-next-month">
          <i class="bi bi-chevron-right"></i>
        </button>
      </div>
      <!-- Header ngày trong tuần -->
      <div class="ot-cal-header">
        <div>T2</div><div>T3</div><div>T4</div><div>T5</div><div>T6</div>
        <div class="ot-cal-we">T7</div><div class="ot-cal-we">CN</div>
      </div>
      <div class="ot-cal-grid">
        ${calCells}
      </div>
    </div>

    <!-- Danh sách đăng ký -->
    <div class="glass-card p-4">
      <h6 class="fw-bold mb-3"><i class="bi bi-list-ul me-2 text-danger"></i>Danh sách đăng ký OT — Tháng ${String(month).padStart(2, '0')}/${year}</h6>
      <div class="table-responsive">
        <table class="table table-hover align-middle mb-0">
          <thead class="table-light">
            <tr>
              <th>Ngày OT</th>
              <th>Giờ</th>
              <th class="text-center">Thực tế</th>
              <th class="text-center">Hệ số</th>
              <th>Trạng thái</th>
              <th class="text-center">Quy đổi</th>
            </tr>
          </thead>
          <tbody>${listRows}</tbody>
        </table>
      </div>
    </div>`;

    // Điều hướng tháng
    document.getElementById('ot-prev-month').onclick = async () => {
      curMonth--; if (curMonth < 1) { curMonth = 12; curYear--; }
      await reloadOT();
    };
    document.getElementById('ot-next-month').onclick = async () => {
      curMonth++; if (curMonth > 12) { curMonth = 1; curYear++; }
      await reloadOT();
    };
  }

  async function reloadOT() {
    const [recs, st2] = await Promise.all([
      api('GET', `/overtime/my?month=${curMonth}&year=${curYear}`),
      api('GET', `/overtime/my/stats?month=${curMonth}&year=${curYear}`),
    ]);
    renderOTPage(curMonth, curYear, recs, st2);
  }

  await reloadOT();

  // ── Native-Style 24h Time Picker (Matches Chrome wheel picker 1:1, NO AM/PM) ──
  function setupNative24hPicker(inputId, popupId, onChangeCallback) {
    const inputEl = document.getElementById(inputId);
    const popupEl = document.getElementById(popupId);
    if (!inputEl || !popupEl) return;

    function renderPicker() {
      let curH = '18';
      let curM = '30';
      const val = (inputEl.value || '').trim();
      if (val && val.includes(':')) {
        const parts = val.split(':');
        const hNum = parseInt(parts[0]);
        const mNum = parseInt(parts[1]);
        if (!isNaN(hNum)) curH = String(Math.min(23, Math.max(0, hNum))).padStart(2, '0');
        if (!isNaN(mNum)) curM = String(Math.min(59, Math.max(0, mNum))).padStart(2, '0');
      }

      let hHtml = '';
      for (let h = 0; h < 24; h++) {
        const hStr = String(h).padStart(2, '0');
        const isSel = (hStr === curH);
        hHtml += `<div class="native-time-item ${isSel ? 'selected' : ''}" data-h="${hStr}">${hStr}</div>`;
      }

      let mHtml = '';
      for (let m = 0; m < 60; m++) {
        const mStr = String(m).padStart(2, '0');
        const isSel = (mStr === curM);
        mHtml += `<div class="native-time-item ${isSel ? 'selected' : ''}" data-m="${mStr}">${mStr}</div>`;
      }

      popupEl.innerHTML = `
        <div class="native-time-col" id="${popupId}-col-h">${hHtml}</div>
        <div class="native-time-col" id="${popupId}-col-m">${mHtml}</div>
      `;

      popupEl.querySelectorAll('[data-h]').forEach(item => {
        item.onclick = (e) => {
          e.stopPropagation();
          curH = item.dataset.h;
          inputEl.value = `${curH}:${curM}`;
          renderPicker();
          scrollSelectedIntoView();
          if (typeof onChangeCallback === 'function') onChangeCallback();
        };
      });

      popupEl.querySelectorAll('[data-m]').forEach(item => {
        item.onclick = (e) => {
          e.stopPropagation();
          curM = item.dataset.m;
          inputEl.value = `${curH}:${curM}`;
          popupEl.classList.add('d-none');
          if (typeof onChangeCallback === 'function') onChangeCallback();
        };
      });
    }

    function scrollSelectedIntoView() {
      setTimeout(() => {
        const selH = popupEl.querySelector('[data-h].selected');
        const selM = popupEl.querySelector('[data-m].selected');
        if (selH) selH.scrollIntoView({ block: 'center' });
        if (selM) selM.scrollIntoView({ block: 'center' });
      }, 20);
    }

    function togglePopup(e) {
      if (e) e.stopPropagation();
      document.querySelectorAll('.native-time-picker-popup').forEach(p => {
        if (p !== popupEl) p.classList.add('d-none');
      });
      const isHidden = popupEl.classList.contains('d-none');
      if (isHidden) {
        renderPicker();
        popupEl.classList.remove('d-none');
        scrollSelectedIntoView();
      } else {
        popupEl.classList.add('d-none');
      }
    }

    inputEl.onclick = togglePopup;
    inputEl.oninput = () => {
      if (typeof onChangeCallback === 'function') onChangeCallback();
    };

    const clockBtn = document.querySelector(`.time-clock-btn[data-target="${inputId}"]`);
    if (clockBtn) clockBtn.onclick = togglePopup;

    document.addEventListener('click', (e) => {
      if (!popupEl.contains(e.target) && e.target !== inputEl && (!clockBtn || !clockBtn.contains(e.target))) {
        popupEl.classList.add('d-none');
      }
    });
  }

  // Khởi tạo bộ chọn giờ Native 24h
  setupNative24hPicker('ot-start', 'picker-ot-start', () => updateOTCalcPreview());
  setupNative24hPicker('ot-end', 'picker-ot-end', () => updateOTCalcPreview());

  // ── Modal xem Lịch sử / Chi tiết OT theo ngày ──
  window.openOTDayHistoryModal = async (dateStr) => {
    const DOW_NAMES = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];
    const parts = dateStr.split('-');
    const dObj = new Date(parseInt(parts[0]), parseInt(parts[1]) - 1, parseInt(parts[2]));
    const dowStr = DOW_NAMES[dObj.getDay()];
    const titleDate = `${dowStr}, ${parts[2]}/${parts[1]}/${parts[0]}`;

    document.getElementById('modal-ot-day-title').innerHTML = `
      <i class="bi bi-calendar-check text-danger me-2"></i>Lịch sử OT ngày ${titleDate}
    `;

    const records = await api('GET', `/overtime/my?month=${curMonth}&year=${curYear}`);
    const dayRecs = records.filter(r => r.work_date === dateStr);
    const bodyEl = document.getElementById('modal-ot-day-body');

    if (dayRecs.length === 0) {
      bodyEl.innerHTML = `<div class="text-center text-muted py-4">Chưa có đăng ký OT nào trong ngày này.</div>`;
    } else {
      let totalRaw = 0;
      let totalWeighted = 0;
      let html = '<div class="d-flex flex-column gap-2">';
      dayRecs.forEach(r => {
        totalRaw += r.raw_hours;
        totalWeighted += r.weighted_hours;
        const isNight = (r.factor === 2.1 || r.factor === 2.7 || r.factor === 3.9);
        const shiftBadge = isNight
          ? `<span class="badge" style="background:#1e293b;color:#f8fafc;font-size:0.72rem"><i class="bi bi-moon-stars-fill text-warning me-1"></i>Ban đêm</span>`
          : `<span class="badge" style="background:#fef3c7;color:#92400e;border:1px solid #fde68a;font-size:0.72rem"><i class="bi bi-sun-fill text-warning me-1"></i>Ban ngày</span>`;

        let actionBtns = '';
        if (r.status !== 'Approved') {
          actionBtns = `
            <div class="mt-2 d-flex gap-2 justify-content-end">
              <button class="btn btn-sm btn-outline-secondary py-0" onclick="bootstrap.Modal.getOrCreateInstance(document.getElementById('modal-ot-day-history')).hide(); openOTEditModal(${r.id});">
                <i class="bi bi-pencil me-1"></i>Sửa
              </button>
              <button class="btn btn-sm btn-outline-danger py-0" onclick="bootstrap.Modal.getOrCreateInstance(document.getElementById('modal-ot-day-history')).hide(); deleteOT(${r.id});">
                <i class="bi bi-trash me-1"></i>Xóa
              </button>
            </div>
          `;
        }

        const rejectInfo = (r.status === 'Rejected' && r.reject_reason)
          ? `<div class="mt-1 small text-danger"><i class="bi bi-exclamation-circle me-1"></i><strong>Lý do từ chối:</strong> ${r.reject_reason}</div>`
          : '';

        html += `
        <div class="p-3 rounded border" style="background:#f8fafc">
          <div class="d-flex justify-content-between align-items-center mb-1">
            <span class="fw-bold text-dark fs-6">
              <i class="bi bi-clock me-1 text-primary"></i>${formatTime24h(r.start_time)} – ${formatTime24h(r.end_time)}
            </span>
            <span>${otBadge(r.status)}</span>
          </div>
          <div class="d-flex align-items-center gap-2 mb-2">
            ${shiftBadge}
            <span class="badge bg-primary-subtle text-primary border border-primary-subtle">Hệ số ${r.factor}x</span>
            <span class="fw-semibold text-primary small">${r.raw_hours}h thực tế → ${r.weighted_hours}h quy đổi</span>
          </div>
          <div class="small text-muted mb-1">
            <strong>Lý do:</strong> ${r.reason || '<em>(Không có ghi chú)</em>'}
          </div>
          ${rejectInfo}
          ${actionBtns}
        </div>`;
      });
      html += `
        <div class="p-2 rounded mt-2 d-flex justify-content-between small fw-bold" style="background:rgba(30,90,200,0.08);border:1px solid rgba(30,90,200,0.15)">
          <span>Tổng trong ngày:</span>
          <span class="text-primary">${totalRaw.toFixed(1)}h thực tế → ${totalWeighted.toFixed(2)}h quy đổi</span>
        </div>
      </div>`;
      bodyEl.innerHTML = html;
    }

    document.getElementById('btn-add-more-ot-day').onclick = () => {
      bootstrap.Modal.getOrCreateInstance(document.getElementById('modal-ot-day-history')).hide();
      openOTRegisterModal(dateStr);
    };

    bootstrap.Modal.getOrCreateInstance(document.getElementById('modal-ot-day-history')).show();
  };

  // ── Modal đăng ký ──
  window.openOTRegisterModal = (dateStr) => {
    document.getElementById('ot-edit-id').value = '';
    document.getElementById('modal-ot-title').textContent = 'Đăng ký OT';
    document.getElementById('btn-save-ot').textContent = 'Đăng ký';

    const targetDate = dateStr || new Date().toISOString().split('T')[0];
    document.getElementById('ot-date').value = targetDate;
    if (document.getElementById('ot-end-date')) {
      document.getElementById('ot-end-date').value = '';
    }

    // Đặt giờ mặc định theo loại ngày
    const dObj = new Date(targetDate + 'T00:00:00');
    const dow = dObj.getDay(); // 0=CN, 6=T7
    const isWeekend = dow === 0 || dow === 6;
    document.getElementById('ot-start').value = isWeekend ? '08:00' : '18:30';
    document.getElementById('ot-end').value = '';

    document.getElementById('ot-reason').value = '';
    document.getElementById('ot-is-holiday').checked = false;
    document.getElementById('ot-calc-preview').classList.add('d-none');
    document.getElementById('ot-time-warning').classList.add('d-none');
    bootstrap.Modal.getOrCreateInstance(document.getElementById('modal-ot-register')).show();
  };

  window.openOTEditModal = async (id) => {
    const records = await api('GET', `/overtime/my?month=${curMonth}&year=${curYear}`);
    const r = records.find(x => x.id === id);
    if (!r) return;
    document.getElementById('ot-edit-id').value = id;
    document.getElementById('modal-ot-title').textContent = 'Sửa đăng ký OT';
    document.getElementById('btn-save-ot').textContent = 'Cập nhật';
    document.getElementById('ot-date').value = r.work_date;
    if (document.getElementById('ot-end-date')) {
      document.getElementById('ot-end-date').value = '';
    }

    document.getElementById('ot-start').value = formatTime24h(r.start_time);
    document.getElementById('ot-end').value = formatTime24h(r.end_time);

    document.getElementById('ot-is-holiday').checked = (r.factor >= 3.0);
    document.getElementById('ot-reason').value = r.reason || '';
    document.getElementById('ot-time-warning').classList.add('d-none');
    updateOTCalcPreview();
    bootstrap.Modal.getOrCreateInstance(document.getElementById('modal-ot-register')).show();
  };

  window.deleteOT = async (id) => {
    const ok = await showConfirm({
      title: 'Xóa đăng ký OT',
      message: 'Bạn có chắc chắn muốn xóa đăng ký OT này không?',
      okText: 'Xóa đăng ký',
      type: 'danger'
    });
    if (!ok) return;
    try {
      await api('DELETE', `/overtime/${id}`);
      toast('Đã xóa đăng ký OT.', 'success');
      await reloadOT();
    } catch (e) { toast(e.message, 'error'); }
  };

  // ── Calc preview (hỗ trợ phân tách theo nhiều ngày & nhiều khung giờ hệ số, ban ngày / ban đêm) ──
  let previewTimer = null;

  function formatDateYMD(d) {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  function parseTimeMinutesLocal(t) {
    if (!t || !t.includes(':')) return 0;
    const parts = t.split(':').map(Number);
    return (parts[0] || 0) * 60 + (parts[1] || 0);
  }

  function getOTDateList(startDateStr, endDateStr, startTime, endTime) {
    if (!startDateStr) return [];
    if (!endDateStr || endDateStr <= startDateStr) {
      return [startDateStr];
    }
    const sMin = parseTimeMinutesLocal(startTime);
    const eMin = parseTimeMinutesLocal(endTime);
    // Nếu endTime <= startTime: ca làm việc qua đêm (VD: 18:30 -> 06:30)
    const isOvernight = (eMin <= sMin && sMin > 0);

    const startD = new Date(startDateStr + 'T00:00:00');
    const endD = new Date(endDateStr + 'T00:00:00');

    if (isOvernight) {
      // Với ca qua đêm (VD: 18:30 ngày 18 đến 06:30 ngày 19):
      // - Nếu chọn Từ ngày 18 đến ngày 19 (hoặc 18 đến 18) -> Chỉ là 1 ca 12 tiếng duy nhất bắt đầu tối 18 kết thúc sáng 19!
      // - Nếu chọn Từ ngày 18 đến ngày 21 -> 3 ca đêm: tối 18->sáng 19, tối 19->sáng 20, tối 20->sáng 21 (kết thúc đúng sáng 21, không sang ngày 22)
      const list = [];
      let cur = new Date(startD);
      const lastStartD = new Date(endD);
      lastStartD.setDate(lastStartD.getDate() - 1);
      while (cur <= lastStartD) {
        list.push(formatDateYMD(cur));
        cur.setDate(cur.getDate() + 1);
      }
      return list.length > 0 ? list : [startDateStr];
    } else {
      // Ca trong ngày (VD: 18:30 -> 21:30): Mỗi ngày từ startDate đến endDate là 1 ca
      const list = [];
      let cur = new Date(startD);
      while (cur <= endD) {
        list.push(formatDateYMD(cur));
        cur.setDate(cur.getDate() + 1);
      }
      return list;
    }
  }

  async function updateOTCalcPreview() {
    const dStr = document.getElementById('ot-date').value;
    const endDateStr = document.getElementById('ot-end-date')?.value || '';
    const s = document.getElementById('ot-start').value;
    const e = document.getElementById('ot-end').value;
    const isHoliday = document.getElementById('ot-is-holiday').checked;
    const preview = document.getElementById('ot-calc-preview');
    const warning = document.getElementById('ot-time-warning');
    const warningTxt = document.getElementById('ot-time-warning-text');

    if (!s || !e || !dStr) { preview.classList.add('d-none'); return; }

    warning.classList.add('d-none');

    clearTimeout(previewTimer);
    previewTimer = setTimeout(async () => {
      try {
        let totalRawAll = 0;
        let totalWeightedAll = 0;
        let totalDayRaw = 0;
        let totalNightRaw = 0;
        const DOW_NAMES = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];

        const data = await api('POST', '/overtime/preview', {
          work_date: dStr,
          end_date: endDateStr || null,
          start_time: s,
          end_time: e,
          is_holiday: isHoliday,
        });

        const allSegs = data.segments || [];
        const segsByDate = {};
        allSegs.forEach(seg => {
          const wDate = (typeof seg.work_date === 'string') ? seg.work_date : (seg.work_date ? seg.work_date.toString() : dStr);
          if (!segsByDate[wDate]) segsByDate[wDate] = [];
          segsByDate[wDate].push(seg);
        });

        let html = '';
        const sortedDates = Object.keys(segsByDate).sort();
        sortedDates.forEach(dtStr => {
          const parts = dtStr.split('-');
          const dObj = new Date(parseInt(parts[0]), parseInt(parts[1]) - 1, parseInt(parts[2]));
          const dowStr = DOW_NAMES[dObj.getDay()];
          const dateSegs = segsByDate[dtStr];

          const segStr = dateSegs.map(seg => {
            const isNight = (seg.shift_type === 'night' || seg.factor === 2.1 || seg.factor === 2.7 || seg.factor === 3.9);
            if (isNight) {
              totalNightRaw += seg.raw_hours;
            } else {
              totalDayRaw += seg.raw_hours;
            }
            totalRawAll += seg.raw_hours;
            totalWeightedAll += seg.weighted_hours;

            const shiftBadge = isNight
              ? `<span class="badge" style="background:#1e293b;color:#f8fafc;font-size:0.72rem"><i class="bi bi-moon-stars-fill text-warning me-1"></i>Ban đêm</span>`
              : `<span class="badge" style="background:#fef3c7;color:#92400e;border:1px solid #fde68a;font-size:0.72rem"><i class="bi bi-sun-fill text-warning me-1"></i>Ban ngày</span>`;

            return `
            <div class="d-flex justify-content-between align-items-center small py-1 border-bottom border-light">
              <span class="text-secondary">
                <i class="bi bi-clock me-1"></i>${seg.start_time} – ${seg.end_time}
                <span class="ms-1">${shiftBadge}</span>
              </span>
              <span>
                <span class="badge" style="background:rgba(30,90,200,.12);color:#1a5abf;border:1px solid rgba(30,90,200,.25)">Hệ số ${seg.factor}x</span>
                <strong class="ms-1 text-primary">${seg.raw_hours}h → ${seg.weighted_hours}h</strong>
              </span>
            </div>`;
          }).join('');

          html += `
          <div class="mb-2 p-2 rounded" style="background:#fff;border:1px solid #e2e8f0">
            <div class="fw-bold small text-dark mb-1">
              <i class="bi bi-calendar-event me-1 text-danger"></i>${dowStr}, ${parts[2]}/${parts[1]}/${parts[0]}
            </div>
            ${segStr}
          </div>`;
        });

        document.getElementById('ot-segments-preview').innerHTML = html;
        document.getElementById('ot-raw-preview').innerHTML = `${totalRawAll.toFixed(1)}h <small class="text-muted fw-normal">(☀️ Ngày: ${totalDayRaw.toFixed(1)}h | 🌙 Đêm: ${totalNightRaw.toFixed(1)}h)</small>`;
        document.getElementById('ot-weighted-preview').textContent = `${totalWeightedAll.toFixed(2)}h quy đổi`;
        preview.classList.remove('d-none');
      } catch (err) {
        preview.classList.add('d-none');
      }
    }, 350);
  }

  document.getElementById('ot-date').addEventListener('change', updateOTCalcPreview);
  document.getElementById('ot-end-date')?.addEventListener('change', updateOTCalcPreview);
  document.getElementById('ot-start').addEventListener('change', updateOTCalcPreview);
  document.getElementById('ot-end').addEventListener('change', updateOTCalcPreview);
  document.getElementById('ot-is-holiday').addEventListener('change', () => {
    const isHoliday = document.getElementById('ot-is-holiday').checked;
    const sElem = document.getElementById('ot-start');
    if (isHoliday && sElem.value === '18:30') {
      sElem.value = '08:00';
    }
    updateOTCalcPreview();
  });

  // ── Save OT ──
  document.getElementById('btn-save-ot').onclick = async () => {
    const editId = document.getElementById('ot-edit-id').value;
    const startDateStr = document.getElementById('ot-date').value;
    const endDateStr = document.getElementById('ot-end-date')?.value || '';
    const startTime = document.getElementById('ot-start').value;
    const endTime = document.getElementById('ot-end').value;
    const isHoliday = document.getElementById('ot-is-holiday').checked;

    if (!startDateStr || !startTime || !endTime) {
      toast('Vui lòng điền đầy đủ ngày và giờ OT.', 'error'); return;
    }

    const baseReason = document.getElementById('ot-reason').value.trim() || null;

    try {
      if (editId) {
        await api('PUT', `/overtime/${editId}`, {
          start_time: startTime,
          end_time: endTime,
          is_holiday: isHoliday,
          reason: baseReason,
        });
        toast('Đã cập nhật đăng ký OT. Trạng thái chuyển về Chờ duyệt.', 'success');
      } else {
        const res = await api('POST', '/overtime', {
          work_date: startDateStr,
          end_date: endDateStr || null,
          start_time: startTime,
          end_time: endTime,
          is_holiday: isHoliday,
          reason: baseReason,
        });
        const segCount = res.segments || 1;
        toast(`Đăng ký OT thành công! Đã tự động tạo ${segCount} đoạn OT theo hệ số quy định.`, 'success');
      }
      bootstrap.Modal.getOrCreateInstance(document.getElementById('modal-ot-register')).hide();
      await reloadOT();
    } catch (e) { toast(e.message, 'error'); }
  };
}


// ══════════════════════════════════════════════════════════════════════════════
// ADMIN — Quản lý OT
// ══════════════════════════════════════════════════════════════════════════════
async function renderManageOT(area) {
  const now = new Date();
  let filterMonth = now.getMonth() + 1;
  let filterYear = now.getFullYear();
  let filterStatus = '';
  let filterProject = '';
  let filterName = '';
  let activeTab = 'list'; // 'list' | 'summary'
  let selectedIds = new Set();

  async function reload() {
    const params = new URLSearchParams({
      month: filterMonth, year: filterYear,
      ...(filterStatus ? { status: filterStatus } : {}),
      ...(filterProject ? { project: filterProject } : {}),
      ...(filterName ? { employee_name: filterName } : {}),
    });
    const records = await api('GET', `/overtime/admin/list?${params}`);
    renderListTab(records);
  }

  async function reloadSummary() {
    const params = new URLSearchParams({
      month: filterMonth, year: filterYear,
      ...(filterProject ? { project: filterProject } : {}),
      ...(filterName ? { employee_name: filterName } : {}),
    });
    const data = await api('GET', `/overtime/admin/summary?${params}`);
    renderSummaryTab(data);
  }

  function buildHeaderTabs() {
    const listActive = activeTab === 'list';
    return `
    <div class="mb-3">
      <ul class="nav nav-tabs" id="ot-tabs">
        <li class="nav-item">
          <button class="nav-link ${listActive ? 'active' : ''}" id="ot-top-tab-list" type="button">
            <i class="bi bi-clock-history me-1"></i>Quản lý OT
          </button>
        </li>
        <li class="nav-item">
          <button class="nav-link ${!listActive ? 'active' : ''}" id="ot-top-tab-summary" type="button">
            <i class="bi bi-table me-1"></i>Bảng tổng hợp
          </button>
        </li>
      </ul>
    </div>`;
  }

  function updateTopTabsUI() {
    const btnList = document.getElementById('ot-top-tab-list');
    const btnSum = document.getElementById('ot-top-tab-summary');
    if (!btnList || !btnSum) return;
    const listActive = activeTab === 'list';
    if (listActive) {
      btnList.classList.add('active');
      btnSum.classList.remove('active');
    } else {
      btnList.classList.remove('active');
      btnSum.classList.add('active');
    }
  }

  function buildFilters() {
    return `
    <div class="filter-bar mb-3 py-2 px-3">
      <div class="d-flex align-items-center gap-2 flex-wrap">
        <div class="d-flex align-items-center gap-1">
          <label class="form-label mb-0 small text-nowrap fw-semibold text-muted">Tháng:</label>
          <select class="form-select form-select-sm" id="ot-f-month" style="width:75px">
            ${Array.from({ length: 12 }, (_, i) => `<option value="${i + 1}" ${i + 1 === filterMonth ? 'selected' : ''}>${String(i + 1).padStart(2, '0')}</option>`).join('')}
          </select>
        </div>
        <div class="d-flex align-items-center gap-1">
          <label class="form-label mb-0 small text-nowrap fw-semibold text-muted">Năm:</label>
          <input type="number" class="form-control form-control-sm" id="ot-f-year" value="${filterYear}" min="2020" max="2099" style="width:80px" />
        </div>
        <div class="d-flex align-items-center gap-1">
          <label class="form-label mb-0 small text-nowrap fw-semibold text-muted">Dự án:</label>
          <input type="text" class="form-control form-control-sm" id="ot-f-project" value="${filterProject}" placeholder="Tên dự án..." style="width:130px" />
        </div>
        <div class="d-flex align-items-center gap-1">
          <label class="form-label mb-0 small text-nowrap fw-semibold text-muted">Tên NV:</label>
          <input type="text" class="form-control form-control-sm" id="ot-f-name" value="${filterName}" placeholder="Họ và tên..." style="width:135px" />
        </div>
        <div class="d-flex align-items-center gap-1">
          <label class="form-label mb-0 small text-nowrap fw-semibold text-muted">Trạng thái:</label>
          <select class="form-select form-select-sm" id="ot-f-status" style="width:110px">
            <option value="" ${!filterStatus ? 'selected' : ''}>Tất cả</option>
            <option value="Pending"  ${filterStatus === 'Pending' ? 'selected' : ''}>Chờ duyệt</option>
            <option value="Approved" ${filterStatus === 'Approved' ? 'selected' : ''}>Đã duyệt</option>
            <option value="Rejected" ${filterStatus === 'Rejected' ? 'selected' : ''}>Từ chối</option>
          </select>
        </div>
        <div class="d-flex gap-1 ms-auto align-items-center">
          <button class="btn btn-danger btn-sm px-3" id="ot-filter-btn">
            <i class="bi bi-search me-1"></i>Lọc
          </button>
          <button class="btn btn-outline-danger btn-sm px-3 text-nowrap" id="ot-export-btn" title="Xuất Excel">
            <i class="bi bi-file-earmark-excel-fill me-1"></i>Xuất Excel
          </button>
        </div>
      </div>
    </div>`;
  }

  function buildBulkBar() {
    return `
    <div class="d-flex gap-2 mb-3 flex-wrap align-items-center" id="ot-bulk-bar">
      <button class="btn btn-success btn-sm" id="ot-approve-selected">
        <i class="bi bi-check-all me-1"></i>Duyệt các mục đã chọn
      </button>
      <button class="btn btn-outline-success btn-sm" id="ot-approve-all">
        <i class="bi bi-check2-all me-1"></i>Duyệt tất cả Pending
      </button>
      <span class="text-muted small my-auto ms-1" id="ot-selected-count">0 mục đã chọn</span>
    </div>`;
  }

  function renderListTab(records) {
    selectedIds.clear();
    const DOW_NAMES = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];
    const tbody = records.length === 0
      ? `<tr><td colspan="9" class="text-center text-muted py-4">Không có dữ liệu.</td></tr>`
      : records.map(r => {
        let dateFormatted = '—';
        if (r.work_date) {
          const parts = r.work_date.split('-');
          const dObj = new Date(parseInt(parts[0]), parseInt(parts[1]) - 1, parseInt(parts[2]));
          const dowStr = DOW_NAMES[dObj.getDay()];
          dateFormatted = `${dowStr}, ${parts[2]}/${parts[1]}/${parts[0]}`;
        }
        return `
        <tr data-ot-id="${r.id}">
          <td class="text-center">
            ${r.status === 'Pending' ? `<input type="checkbox" class="form-check-input ot-check" data-id="${r.id}">` : ''}
          </td>
          <td class="text-center small fw-semibold">${dateFormatted}</td>
          <td><code style="font-size:0.78rem;color:#475569;background:#f1f5f9;padding:2px 6px;border-radius:4px">${r.employee_code}</code></td>
          <td class="fw-semibold" style="color:#1e293b">${r.full_name}</td>
          <td class="text-muted small">${r.project || '—'}</td>
          <td class="text-center">${formatTime24h(r.start_time)} – ${formatTime24h(r.end_time)}<br><small class="text-muted">${r.raw_hours}h</small></td>
          <td class="text-center"><span class="badge bg-secondary">${r.factor}x</span></td>
          <td>${otBadge(r.status)}${r.reject_reason ? `<br><small class="text-danger">${r.reject_reason}</small>` : ''}</td>
          <td class="text-center text-nowrap">
            ${r.status === 'Pending' ? `
            <button class="btn btn-sm btn-success py-0 px-2 me-1" onclick="adminApproveOT(${r.id})" title="Duyệt">✓</button>
            <button class="btn btn-sm btn-danger py-0 px-2 me-1" onclick="openRejectModal(${r.id})" title="Từ chối">✗</button>
            ` : `
            <button class="btn btn-sm btn-warning py-0 px-2 me-1 text-dark" onclick="adminResetPendingOT(${r.id})" title="Trả về Chưa duyệt"><i class="bi bi-arrow-counterclockwise"></i></button>
            `}
            <button class="btn btn-sm btn-outline-secondary py-0 px-2 me-1" onclick="viewOTDetail(${r.id})" title="Xem chi tiết"><i class="bi bi-eye"></i></button>
            <button class="btn btn-sm btn-outline-danger py-0 px-2" onclick="adminDeleteOT(${r.id})" title="Xóa OT"><i class="bi bi-trash"></i></button>
          </td>
        </tr>`;
      }).join('');

    document.getElementById('ot-tab-content').innerHTML = `
    ${buildBulkBar()}
    <div class="table-wrap">
      <div class="table-responsive">
        <table class="table table-hover align-middle mb-0">
          <thead class="table-light">
            <tr>
              <th style="width:40px"><input type="checkbox" class="form-check-input" id="ot-check-all" title="Chọn tất cả Pending"></th>
              <th>Ngày OT</th><th>MNV</th><th>Họ tên</th><th>Dự án</th>
              <th class="text-center">Giờ</th><th class="text-center">Hệ số</th>
              <th>Trạng thái</th><th class="text-center">Thao tác</th>
            </tr>
          </thead>
          <tbody>${tbody}</tbody>
        </table>
      </div>
    </div>`;

    // Select all checkbox
    document.getElementById('ot-check-all').onchange = function () {
      document.querySelectorAll('.ot-check').forEach(cb => {
        cb.checked = this.checked;
        const id = parseInt(cb.dataset.id);
        this.checked ? selectedIds.add(id) : selectedIds.delete(id);
      });
      updateSelectedCount();
    };
    document.querySelectorAll('.ot-check').forEach(cb => {
      cb.onchange = function () {
        const id = parseInt(this.dataset.id);
        this.checked ? selectedIds.add(id) : selectedIds.delete(id);
        updateSelectedCount();
      };
    });

    document.getElementById('ot-approve-selected').onclick = async () => {
      if (selectedIds.size === 0) { toast('Chưa chọn OT nào.', 'error'); return; }
      const ok = await showConfirm({
        title: 'Duyệt danh sách OT',
        message: `Xác nhận duyệt ${selectedIds.size} OT đã chọn?`,
        okText: 'Duyệt OT',
        type: 'warning'
      });
      if (!ok) return;
      try {
        const res = await api('POST', '/overtime/admin/approve-selected', { ids: [...selectedIds] });
        toast(res.message, 'success');
        await reload();
      } catch (e) { toast(e.message, 'error'); }
    };

    document.getElementById('ot-approve-all').onclick = async () => {
      const ok = await showConfirm({
        title: 'Duyệt toàn bộ OT',
        message: `Duyệt tất cả OT Pending trong tháng ${filterMonth}/${filterYear}?`,
        okText: 'Duyệt tất cả',
        type: 'warning'
      });
      if (!ok) return;
      try {
        const params = new URLSearchParams({ month: filterMonth, year: filterYear, ...(filterProject ? { project: filterProject } : {}) });
        const res = await api('POST', `/overtime/admin/approve-all-pending?${params}`);
        toast(res.message, 'success');
        await reload();
      } catch (e) { toast(e.message, 'error'); }
    };
  }

  function updateSelectedCount() {
    const el = document.getElementById('ot-selected-count');
    if (el) el.textContent = `${selectedIds.size} mục đã chọn`;
  }

  function renderSummaryTab(data) {
    const { rows, num_days, month, year } = data;
    const DOW = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];

    if (!rows || rows.length === 0) {
      document.getElementById('ot-tab-content').innerHTML = `
      <div class="text-center text-muted py-5"><i class="bi bi-inbox me-2"></i>Không có dữ liệu OT Approved trong tháng này.</div>`;
      return;
    }

    // Header số ngày trong tháng + thứ
    const dayHeaders = Array.from({ length: num_days }, (_, i) => {
      const d = i + 1;
      const dow = new Date(year, month - 1, d).getDay();
      const isSunday = dow === 0;
      const isSaturday = dow === 6;
      const bgStyle = isSunday ? 'background:#fce4ec !important;' : (isSaturday ? 'background:#fff7ed !important;' : 'background:#f8fafc;');
      const dowColor = isSunday ? 'color:#d5001c;font-weight:700' : (isSaturday ? 'color:#e65100;font-weight:600' : 'color:#64748b');
      return `<th class="text-center p-1" style="min-width:38px;width:38px;font-size:0.75rem;${bgStyle}">
        <span class="fw-bold" style="color:#1e293b">${d}</span><br>
        <span style="font-size:0.66rem;${dowColor}">${DOW[dow]}</span>
      </th>`;
    }).join('');

    // Danh sách dòng dữ liệu cho bảng
    const tableRows = rows.map((row, idx) => {
      const dayCells = Array.from({ length: num_days }, (_, i) => {
        const d = i + 1;
        const dow = new Date(year, month - 1, d).getDay();
        const isSunday = dow === 0;
        const isSaturday = dow === 6;
        const sundayBg = isSunday ? 'background:#fff0f3;' : (isSaturday ? 'background:#fffbeb;' : '');
        const dayData = row.days[d];
        if (!dayData || dayData.length === 0) {
          return `<td class="text-center p-1" style="font-size:0.75rem;color:#cbd5e1;${sundayBg}">—</td>`;
        }
        const total = dayData.reduce((acc, x) => acc + x.raw_hours, 0);
        const factorsStr = Array.from(new Set(dayData.map(x => `${x.factor}x`))).join(', ');
        const detail = dayData.map(x => `${x.start_time}–${x.end_time} (${x.raw_hours}h × ${x.factor})`).join('\n');
        return `<td class="text-center p-1 ot-summary-day-cell" title="${detail}" style="cursor:pointer;${sundayBg}">
          <div style="line-height:1.2">
            <span style="background:#fef2f2;color:#d5001c;border:1px solid #fecaca;font-size:0.74rem;font-weight:700;padding:1px 4px;border-radius:4px;display:inline-block">${total}h</span>
            <div style="font-size:0.64rem;color:#64748b;font-weight:600;margin-top:1px">${factorsStr}</div>
          </div>
        </td>`;
      }).join('');

      return `
      <tr style="height:48px">
        <td class="col-sticky col-stt text-center text-muted small" style="vertical-align:middle;font-weight:600">${idx + 1}</td>
        <td class="col-sticky col-mnv text-center" style="vertical-align:middle">
          <code style="font-size:0.78rem;color:#475569;background:#f1f5f9;padding:2px 6px;border-radius:4px">${row.employee_code}</code>
        </td>
        <td class="col-sticky col-name text-center fw-bold" style="vertical-align:middle;color:#1e293b;font-size:0.82rem">${row.full_name}</td>
        ${dayCells}
        <td class="text-center fw-bold text-danger p-1 ot-summary-total" style="vertical-align:middle;font-size:0.8rem">${row.total_raw}h</td>
        <td class="text-center fw-bold p-1 ot-summary-quydoi" style="vertical-align:middle;font-size:0.82rem;color:#d5001c">${row.total_weighted}h</td>
      </tr>`;
    }).join('');

    document.getElementById('ot-tab-content').innerHTML = `
    <div class="table-wrapper ot-summary-scroll-container" id="ot-summary-wrap">
      <table class="sticky-table ot-summary-table mb-0" id="ot-summary-table">
        <thead>
          <tr style="height:42px">
            <th class="col-sticky col-stt text-center" style="background:#f8fafc;color:#475569;font-weight:700;font-size:0.78rem;vertical-align:middle">STT</th>
            <th class="col-sticky col-mnv text-center" style="background:#f8fafc;color:#475569;font-weight:700;font-size:0.78rem;vertical-align:middle">Mã NV</th>
            <th class="col-sticky col-name text-center" style="background:#f8fafc;color:#475569;font-weight:700;font-size:0.78rem;vertical-align:middle">Họ và tên</th>
            ${dayHeaders}
            <th class="text-center ot-summary-total" style="vertical-align:middle;color:#d5001c;font-size:0.78rem">Tổng OT</th>
            <th class="text-center ot-summary-quydoi" style="vertical-align:middle;color:#d5001c;font-size:0.78rem">Quy đổi</th>
          </tr>
        </thead>
        <tbody>${tableRows}</tbody>
      </table>
    </div>`;

    // Dynamic sticky positioning logic from huong_dan_lam_bang_cuon_ngang_sticky.md
    setTimeout(() => {
      const table = document.getElementById('ot-summary-table');
      if (!table) return;
      const rows = table.querySelectorAll('tr');
      rows.forEach(row => {
        const cells = row.children;
        let leftOffset = 0;
        for (let i = 0; i < 3; i++) {
          if (!cells[i]) continue;
          cells[i].style.position = 'sticky';
          cells[i].style.left = `${leftOffset}px`;
          leftOffset += cells[i].offsetWidth;
        }
      });
    }, 50);
  }

  // ── Render toàn bộ trang ──
  area.innerHTML = `
  ${buildHeaderTabs()}
  ${buildFilters()}
  <div class="glass-card p-3" style="max-width:100%;min-width:0;overflow:hidden">
    <div id="ot-tab-content" style="max-width:100%;min-width:0">
      <div class="d-flex justify-content-center py-4"><div class="spinner-border text-danger"></div></div>
    </div>
  </div>`;

  // ── Top tab events ──
  document.getElementById('ot-top-tab-list').onclick = async () => {
    if (activeTab === 'list') return;
    activeTab = 'list';
    updateTopTabsUI();
    document.getElementById('ot-tab-content').innerHTML = '<div class="d-flex justify-content-center py-4"><div class="spinner-border text-danger"></div></div>';
    await reload();
  };

  document.getElementById('ot-top-tab-summary').onclick = async () => {
    if (activeTab === 'summary') return;
    activeTab = 'summary';
    updateTopTabsUI();
    document.getElementById('ot-tab-content').innerHTML = '<div class="d-flex justify-content-center py-4"><div class="spinner-border text-danger"></div></div>';
    await reloadSummary();
  };

  // ── Filter events ──
  document.getElementById('ot-filter-btn').onclick = async () => {
    filterMonth = parseInt(document.getElementById('ot-f-month').value);
    filterYear = parseInt(document.getElementById('ot-f-year').value);
    filterProject = document.getElementById('ot-f-project').value.trim();
    filterName = document.getElementById('ot-f-name').value.trim();
    filterStatus = document.getElementById('ot-f-status').value;
    if (activeTab === 'list') await reload();
    else await reloadSummary();
  };

  document.getElementById('ot-export-btn').onclick = async () => {
    const params = new URLSearchParams({
      month: filterMonth, year: filterYear,
      token: STATE.token,
      ...(filterProject ? { project: filterProject } : {}),
    });
    try {
      const res = await fetch(`${API}/overtime/admin/export?${params}`, {
        headers: { 'Authorization': 'Bearer ' + STATE.token }
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.detail || 'Không thể xuất file Excel.');
      }
      const blob = await res.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `OT_T${String(filterMonth).padStart(2, '0')}_${filterYear}.xlsx`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
      toast('Xuất báo cáo Excel thành công!', 'success');
    } catch (e) {
      toast(e.message, 'error');
    }
  };

  // ── Global actions ──
  window.adminApproveOT = async (id) => {
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
      await reload();
    } catch (e) { toast(e.message, 'error'); }
  };

  window.adminResetPendingOT = async (id) => {
    const ok = await showConfirm({
      title: 'Đặt lại trạng thái OT',
      message: 'Xác nhận chuyển trạng thái yêu cầu OT này về Chưa duyệt (Pending)?',
      okText: 'Trả về Chưa duyệt',
      type: 'warning'
    });
    if (!ok) return;
    try {
      await api('POST', `/overtime/admin/${id}/reset-pending`);
      toast('Đã chuyển OT về trạng thái Chưa duyệt.', 'success');
      await reload();
    } catch (e) { toast(e.message, 'error'); }
  };

  window.adminDeleteOT = async (id) => {
    const ok = await showConfirm({
      title: 'Xóa yêu cầu OT',
      message: 'Xác nhận xóa yêu cầu OT này? (Hành động này không thể hoàn tác)',
      okText: 'Xóa OT',
      type: 'danger'
    });
    if (!ok) return;
    try {
      await api('DELETE', `/overtime/admin/${id}`);
      toast('Đã xóa yêu cầu OT.', 'success');
      if (activeTab === 'list') await reload();
      else await reloadSummary();
    } catch (e) { toast(e.message, 'error'); }
  };

  window.openRejectModal = (id) => {
    document.getElementById('reject-ot-id').value = id;
    document.getElementById('reject-reason-text').value = '';
    bootstrap.Modal.getOrCreateInstance(document.getElementById('modal-ot-reject')).show();
  };

  document.getElementById('btn-confirm-reject').onclick = async () => {
    const id = document.getElementById('reject-ot-id').value;
    const reason = document.getElementById('reject-reason-text').value.trim();
    if (!reason) { toast('Vui lòng nhập lý do từ chối.', 'error'); return; }
    try {
      await api('POST', `/overtime/admin/${id}/reject`, { reject_reason: reason });
      toast('Đã từ chối OT.', 'success');
      bootstrap.Modal.getOrCreateInstance(document.getElementById('modal-ot-reject')).hide();
      await reload();
    } catch (e) { toast(e.message, 'error'); }
  };

  window.viewOTDetail = async (id) => {
    const params = new URLSearchParams({ month: filterMonth, year: filterYear });
    const records = await api('GET', `/overtime/admin/list?${params}`);
    const r = records.find(x => x.id === id);
    if (!r) return;

    const DOW_NAMES = ['Chủ nhật', 'Thứ hai', 'Thứ ba', 'Thứ tư', 'Thứ năm', 'Thứ sáu', 'Thứ bảy'];
    let dateFormatted = r.work_date;
    if (r.work_date) {
      const parts = r.work_date.split('-');
      const dObj = new Date(parseInt(parts[0]), parseInt(parts[1]) - 1, parseInt(parts[2]));
      const dowStr = DOW_NAMES[dObj.getDay()];
      dateFormatted = `${dowStr}, ${parts[2]}/${parts[1]}/${parts[0]}`;
    }

    document.getElementById('ot-detail-body').innerHTML = `
    <div class="row g-3">
      <div class="col-6"><div class="text-muted small">Nhân viên</div><div class="fw-semibold">${r.full_name}</div></div>
      <div class="col-6"><div class="text-muted small">Mã NV</div><div class="fw-semibold">${r.employee_code}</div></div>
      <div class="col-6"><div class="text-muted small">Ngày OT</div><div class="fw-semibold">${dateFormatted}</div></div>
      <div class="col-6"><div class="text-muted small">Dự án</div><div class="fw-semibold">${r.project || '—'}</div></div>
      <div class="col-4"><div class="text-muted small">Bắt đầu</div><div class="fw-bold fs-5">${formatTime24h(r.start_time)}</div></div>
      <div class="col-4"><div class="text-muted small">Kết thúc</div><div class="fw-bold fs-5">${formatTime24h(r.end_time)}</div></div>
      <div class="col-4"><div class="text-muted small">Số giờ</div><div class="fw-bold fs-5 text-danger">${r.raw_hours}h</div></div>
      <div class="col-6"><div class="text-muted small">Hệ số</div><div class="fw-semibold">${r.factor}x</div></div>
      <div class="col-6"><div class="text-muted small">Giờ quy đổi</div><div class="fw-bold text-danger">${r.weighted_hours}h</div></div>
      <div class="col-12"><div class="text-muted small">Lý do OT</div><div>${r.reason || '—'}</div></div>
      <div class="col-12"><div class="text-muted small">Trạng thái</div><div>${otBadge(r.status)}</div></div>
      ${r.reject_reason ? `<div class="col-12"><div class="text-muted small">Lý do từ chối</div><div class="text-danger">${r.reject_reason}</div></div>` : ''}
    </div>`;

    let actionBtns = `<button class="btn btn-secondary" data-bs-dismiss="modal">Đóng</button>`;
    if (r.status === 'Pending') {
      actionBtns = `
        <button class="btn btn-success me-1" onclick="bootstrap.Modal.getInstance(document.getElementById('modal-ot-detail')).hide(); adminApproveOT(${r.id})"><i class="bi bi-check-lg me-1"></i>Duyệt OT</button>
        <button class="btn btn-danger me-1" onclick="bootstrap.Modal.getInstance(document.getElementById('modal-ot-detail')).hide(); openRejectModal(${r.id})"><i class="bi bi-x-lg me-1"></i>Từ chối</button>
        <button class="btn btn-secondary" data-bs-dismiss="modal">Đóng</button>`;
    } else {
      actionBtns = `
        <button class="btn btn-warning me-1 text-dark fw-semibold" onclick="bootstrap.Modal.getInstance(document.getElementById('modal-ot-detail')).hide(); adminResetPendingOT(${r.id})"><i class="bi bi-arrow-counterclockwise me-1"></i>Trả về Chưa duyệt</button>
        <button class="btn btn-outline-danger me-1" onclick="bootstrap.Modal.getInstance(document.getElementById('modal-ot-detail')).hide(); adminDeleteOT(${r.id})"><i class="bi bi-trash me-1"></i>Xóa OT</button>
        <button class="btn btn-secondary" data-bs-dismiss="modal">Đóng</button>`;
    }

    document.getElementById('ot-detail-footer').innerHTML = actionBtns;
    bootstrap.Modal.getOrCreateInstance(document.getElementById('modal-ot-detail')).show();
  };

  // Load initial tab
  await reload();
}
