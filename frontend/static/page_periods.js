// ═══════════════════════════════════════════
// MANAGE PERIODS (Admin)
// ═══════════════════════════════════════════
async function renderManagePeriods(area) {
  const periods = await api('GET', '/admin/periods');
  const now = new Date();

  area.innerHTML = `
<div class="section-header">
  <div class="section-title"><i class="bi bi-calendar3 text-info"></i> Quản lý kỳ đăng ký</div>
  <button class="btn btn-primary btn-sm" id="btn-add-period">
    <i class="bi bi-plus-circle-fill me-1"></i>Tạo kỳ mới
  </button>
</div>
<div class="table-wrap">
  <div class="table-responsive">
    <table class="table table-hover">
      <thead><tr>
        <th>Kỳ đăng ký</th><th>Ngày mở</th><th>Ngày đóng</th><th>Trạng thái</th><th style="width:130px">Thao tác</th>
      </tr></thead>
      <tbody>
        ${periods.length ? periods.map(p => `
        <tr>
          <td><strong>Tháng ${p.month}/${p.year}</strong></td>
          <td>${p.open_date ? new Date(p.open_date).toLocaleString('vi-VN',{hour12:false}) : '—'}</td>
          <td>${p.close_date ? new Date(p.close_date).toLocaleString('vi-VN',{hour12:false}) : '—'}</td>
          <td>${badgePeriod(p.status)}</td>
          <td>
            <button class="btn-icon edit me-1" title="Chỉnh sửa" onclick="openEditPeriod(${p.id})">
              <i class="bi bi-pencil-fill"></i>
            </button>
            <button class="btn-icon ${p.status==='open'?'lock':'unlock'} me-1"
              title="${p.status==='open'?'Đóng kỳ':'Mở kỳ'}"
              onclick="togglePeriod(${p.id},'${p.status}')">
              <i class="bi bi-${p.status==='open'?'lock-fill':'unlock-fill'}"></i>
            </button>
            <button class="btn-icon" style="background:rgba(248,81,73,.1);color:var(--danger)" title="Xóa kỳ"
              onclick="deletePeriod(${p.id}, ${p.month}, ${p.year})">
              <i class="bi bi-trash-fill"></i>
            </button>
          </td>
        </tr>`).join('') : `
        <tr><td colspan="5" class="text-center py-5">
          <div class="empty-state"><i class="bi bi-calendar-x"></i><p>Chưa có kỳ đăng ký nào</p></div>
        </td></tr>`}
      </tbody>
    </table>
  </div>
</div>`;

  document.getElementById('btn-add-period').onclick = () => openPeriodModal(null, now);
}

window.openEditPeriod = async (id) => {
  const periods = await api('GET', '/admin/periods');
  openPeriodModal(periods.find(p => p.id === id), null);
};

window.togglePeriod = async (id, currentStatus) => {
  const newStatus = currentStatus === 'open' ? 'closed' : 'open';
  const label = newStatus === 'open' ? 'mở' : 'đóng';
  if (!confirm(`Xác nhận ${label} kỳ đăng ký này?`)) return;
  try {
    await api('PUT', `/admin/periods/${id}`, { status: newStatus });
    toast(`Đã ${label} kỳ đăng ký`);
    navigate('manage-periods');
  } catch (e) { toast(e.message, 'error'); }
};

window.deletePeriod = async (id, month, year) => {
  if (!confirm(`⚠️ Xóa kỳ Tháng ${month}/${year}?\n\nToàn bộ lịch đăng ký trong kỳ này cũng sẽ bị xóa.`)) return;
  try {
    const r = await api('DELETE', `/admin/periods/${id}`);
    toast(r.message, 'success');
    navigate('manage-periods');
  } catch (e) { toast(e.message, 'error'); }
};

function openPeriodModal(period, now) {
  document.getElementById('modal-period-title').textContent = period ? 'Chỉnh sửa kỳ đăng ký' : 'Tạo kỳ đăng ký mới';
  const setV = (id, val) => { const el = document.getElementById(id); if (el && val != null) el.value = val; };
  const toLocal = (dt) => dt ? new Date(dt).toISOString().slice(0, 16) : '';
  setV('period-id', period?.id || '');
  setV('p-month', period?.month ?? (now ? now.getMonth() + 1 : 1));
  setV('p-year', period?.year ?? (now ? now.getFullYear() : 2025));
  setV('p-open', toLocal(period?.open_date));
  setV('p-close', toLocal(period?.close_date));
  setV('p-status', period?.status ?? 'closed');
  ['p-month','p-year'].forEach(id => { document.getElementById(id).disabled = !!period; });
  bootstrap.Modal.getOrCreateInstance(document.getElementById('modal-period')).show();
}

document.getElementById('btn-save-period').addEventListener('click', async () => {
  const id = document.getElementById('period-id').value;
  const toISO = (v) => v ? new Date(v).toISOString() : null;
  const payload = {
    open_date: toISO(document.getElementById('p-open').value),
    close_date: toISO(document.getElementById('p-close').value),
    status: document.getElementById('p-status').value,
  };
  try {
    if (id) {
      await api('PUT', `/admin/periods/${id}`, payload);
      toast('Cập nhật kỳ đăng ký thành công');
    } else {
      await api('POST', '/admin/periods', {
        month: parseInt(document.getElementById('p-month').value),
        year: parseInt(document.getElementById('p-year').value),
        ...payload,
      });
      toast('Đã tạo kỳ đăng ký');
    }
    bootstrap.Modal.getInstance(document.getElementById('modal-period')).hide();
    navigate('manage-periods');
  } catch (e) { toast(e.message, 'error'); }
});
