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
    <button class="btn btn-sm btn-outline-success" id="btn-export" ${!data.period?'disabled':''}>
      <i class="bi bi-file-earmark-excel-fill me-1"></i>Xuất Excel
    </button>
    <button class="btn btn-sm btn-outline-secondary" id="btn-download-tpl" ${!data.period?'disabled':''}>
      <i class="bi bi-download me-1"></i>Tải file mẫu
    </button>
    <div class="dropdown">
      <button class="btn btn-sm btn-outline-primary dropdown-toggle" type="button" data-bs-toggle="dropdown" ${!data.period?'disabled':''}>
        <i class="bi bi-upload me-1"></i>Import Lịch
      </button>
      <ul class="dropdown-menu">
        <li><a class="dropdown-item" href="#" onclick="document.getElementById('import-sched-file').click()"><i class="bi bi-file-excel me-2"></i>Từ file Excel</a></li>
        <li><a class="dropdown-item" href="#" onclick="promptSchedImportLink()"><i class="bi bi-link-45deg me-2"></i>Từ Google Sheets</a></li>
      </ul>
    </div>
    <input type="file" id="import-sched-file" accept=".xlsx" style="display:none" onchange="handleSchedImportExcel(event)">
    ${!data.period ? `<button class="btn btn-sm btn-danger" id="btn-create-period"><i class="bi bi-plus-circle me-1"></i>Tạo kỳ tháng này</button>` : ''}
  </div>
</div>

${data.period ? `
<div class="d-flex gap-3 mb-3 align-items-center flex-wrap">
  <span>${badgePeriod(data.period.status)}</span>
  <span class="small text-muted">Tháng ${data.period.month}/${data.period.year}</span>
  <span class="small text-muted">Đăng ký: <strong class="text-info">${registeredCount}/${data.rows.length}</strong> TTS</span>
  <button class="btn btn-sm btn-outline-danger" onclick="deleteSchedPeriod(${data.period.id})">
    <i class="bi bi-trash me-1"></i>Xóa kỳ
  </button>
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

    if (data.period) {
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

      document.getElementById('btn-download-tpl').onclick = () => {
        const url = API + `/admin/schedule/import-template?period_id=${data.period.id}`;
        fetch(url, { headers: { 'Authorization': 'Bearer ' + STATE.token } })
          .then(res => res.blob())
          .then(blob => {
            const blobUrl = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = blobUrl;
            a.download = `mau_import_lich_t${selMonth}_${selYear}.xlsx`;
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(blobUrl);
          }).catch(() => toast('Lỗi khi tải mẫu', 'error'));
      };

      window.handleSchedImportExcel = async function (event) {
        const file = event.target.files[0];
        if (!file) return;
        const formData = new FormData();
        formData.append('file', file);
        toast('Đang xử lý file...', 'info');
        event.target.value = '';
        try {
          const res = await fetch(API + `/admin/schedule/import?period_id=${data.period.id}`, {
            method: 'POST',
            headers: { 'Authorization': 'Bearer ' + STATE.token },
            body: formData,
          });
          const resData = await res.json();
          if (!res.ok) throw new Error(resData.detail || 'Lỗi khi nhập dữ liệu');
          toast(resData.message, 'success');
          load();
        } catch (err) {
          toast(err.message, 'error');
        }
      };

      window.promptSchedImportLink = async function () {
        const url = prompt('Nhập đường dẫn Google Sheets (phải được chia sẻ công khai "Bất kỳ ai có liên kết"):');
        if (!url) return;
        toast('Đang tải danh sách sheet...', 'info');
        try {
          const res = await api('POST', `/admin/schedule/import-link-sheets`, { url });
          if (!res.sheets || res.sheets.length === 0) {
            throw new Error('Không tìm thấy sheet nào trong file.');
          }
          
          const select = document.getElementById('sheet-select');
          select.innerHTML = '';
          res.sheets.forEach(sheet => {
            const opt = document.createElement('option');
            opt.value = sheet;
            opt.textContent = sheet;
            select.appendChild(opt);
          });
          
          const modal = new bootstrap.Modal(document.getElementById('modal-select-sheet'));
          modal.show();
          
          const confirmBtn = document.getElementById('btn-confirm-sheet');
          confirmBtn.onclick = async () => {
            modal.hide();
            const sheet_name = select.value;
            toast('Đang xử lý dữ liệu import...', 'info');
            try {
              const importRes = await api('POST', `/admin/schedule/import-link?period_id=${data.period.id}`, { url, sheet_name });
              toast(importRes.message, 'success');
              load();
            } catch (e) {
              toast(e.message, 'error');
            }
          };
        } catch (e) {
          toast(e.message, 'error');
        }
      };

      window.deleteSchedPeriod = async function (periodId) {
        if (!confirm(`Xóa kỳ tháng ${selMonth}/${selYear}?\n\nToàn bộ lịch đã import sẽ bị xóa.`)) return;
        try {
          await api('DELETE', `/admin/periods/${periodId}`);
          toast('Đã xóa kỳ đăng ký', 'success');
          load();
        } catch (e) { toast(e.message, 'error'); }
      };

    } else {
      // Không có kỳ — nút Tạo kỳ tháng này
      const btnCreate = document.getElementById('btn-create-period');
      if (btnCreate) {
        btnCreate.onclick = async () => {
          try {
            await api('POST', '/admin/periods', { month: selMonth, year: selYear, status: 'closed' });
            toast(`Đã tạo kỳ tháng ${selMonth}/${selYear}`, 'success');
            load();
          } catch (e) { toast(e.message, 'error'); }
        };
      }
    }
  }

  await load();
}
