/* ═══════════════════════════════════════════════
   VIETTEL INTERN & EMPLOYEE MANAGEMENT — FRONTEND
   ═══════════════════════════════════════════════ */

const API = (window.location.protocol.startsWith('http')) ? window.location.origin : 'http://localhost:8088';
let STATE = { token: null, role: null, user_type: null, userId: null, fullName: null };

// ── Persist session ──
function saveSession(data) {
  STATE = {
    token: data.access_token,
    role: data.role,
    user_type: data.user_type || 'intern',
    userId: data.user_id,
    fullName: data.full_name,
  };
  localStorage.setItem('intern_session', JSON.stringify(STATE));
}
function loadSession() {
  try {
    const s = JSON.parse(localStorage.getItem('intern_session'));
    if (s?.token) { STATE = s; return true; }
  } catch { }
  return false;
}
function clearSession() { STATE = {}; localStorage.removeItem('intern_session'); }

// ── API helper ──
async function api(method, path, body) {
  // Normalize path by stripping trailing slash for API endpoints
  if (path && path.length > 1 && path.includes('/') && path.endsWith('/')) {
    path = path.replace(/\/+$/, '');
  }
  const opts = { method, headers: { 'Content-Type': 'application/json' } };
  if (STATE.token) opts.headers['Authorization'] = 'Bearer ' + STATE.token;
  if (body !== undefined) opts.body = JSON.stringify(body);
  const res = await fetch(API + path, opts);
  if (res.status === 204) return null;
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    let msg = 'Lỗi máy chủ';
    if (typeof data.detail === 'string') msg = data.detail;
    else if (Array.isArray(data.detail)) msg = data.detail.map(d => d.msg || JSON.stringify(d)).join(', ');
    throw new Error(msg);
  }
  return data;
}

// ── Toast (Thông báo góc dưới bên trái) ──
function toast(msg, type = 'success', delay = 3500) {
  const el = document.getElementById('app-toast');
  const msgEl = document.getElementById('toast-msg');
  const iconWrap = document.getElementById('toast-icon');

  if (!el || !msgEl) return;

  const ICONS = {
    success: '<i class="bi bi-check-circle-fill fs-5"></i>',
    error: '<i class="bi bi-x-circle-fill fs-5"></i>',
    danger: '<i class="bi bi-x-circle-fill fs-5"></i>',
    warning: '<i class="bi bi-exclamation-triangle-fill fs-5"></i>',
    info: '<i class="bi bi-info-circle-fill fs-5"></i>'
  };

  const actualType = type === 'danger' ? 'error' : type;
  el.className = `toast align-items-center border-0 ${actualType}`;
  if (iconWrap) iconWrap.innerHTML = ICONS[actualType] || ICONS.info;
  msgEl.textContent = msg;

  const toastInstance = bootstrap.Toast.getOrCreateInstance(el, { delay });
  toastInstance.show();
}

// Ghi đè alert mặc định của web để hiển thị bằng Toast góc dưới bên trái
window.alert = function (msg) {
  toast(msg, 'info', 4500);
};

// ── Confirm Modal (Hộp thoại xác nhận ở giữa màn hình) ──
function showConfirm(options = {}) {
  return new Promise((resolve) => {
    const modalEl = document.getElementById('app-confirm-modal');
    if (!modalEl) {
      resolve(confirm(typeof options === 'string' ? options : options.message || 'Xác nhận?'));
      return;
    }

    const title = typeof options === 'string' ? 'Xác nhận thao tác' : (options.title || 'Xác nhận thao tác');
    const message = typeof options === 'string' ? options : (options.message || 'Bạn có chắc chắn muốn thực hiện thao tác này?');
    const okText = options.okText || 'Xác nhận';
    const cancelText = options.cancelText || 'Hủy bỏ';
    const type = options.type || 'warning'; // 'danger', 'warning', 'info', 'success'

    document.getElementById('confirm-modal-title').textContent = title;
    document.getElementById('confirm-modal-message').textContent = message;

    // Purge old event listeners by cloning buttons
    const oldOk = document.getElementById('confirm-modal-ok-btn');
    const oldCancel = document.getElementById('confirm-modal-cancel-btn');
    const okBtn = oldOk.cloneNode(true);
    const cancelBtn = oldCancel.cloneNode(true);
    oldOk.parentNode.replaceChild(okBtn, oldOk);
    oldCancel.parentNode.replaceChild(cancelBtn, oldCancel);

    okBtn.textContent = okText;
    cancelBtn.textContent = cancelText;

    if (options.hideCancel) {
      cancelBtn.classList.add('d-none');
    } else {
      cancelBtn.classList.remove('d-none');
    }

    const iconBg = document.getElementById('confirm-modal-icon-bg');
    const iconEl = document.getElementById('confirm-modal-icon');

    // Config Icon & Colors based on type
    if (iconBg && iconEl) {
      iconBg.className = 'confirm-icon-wrap mx-auto mb-3 confirm-icon-' + type;
      if (type === 'danger') {
        iconEl.className = 'bi bi-trash3-fill';
        okBtn.className = 'btn btn-danger px-4 py-2 rounded-3 fw-semibold';
      } else if (type === 'warning') {
        iconEl.className = 'bi bi-exclamation-triangle-fill';
        okBtn.className = 'btn btn-warning text-white px-4 py-2 rounded-3 fw-semibold';
      } else if (type === 'success') {
        iconEl.className = 'bi bi-check-circle-fill text-success';
        okBtn.className = 'btn btn-success text-white px-4 py-2 rounded-3 fw-semibold';
      } else {
        iconEl.className = 'bi bi-info-circle-fill';
        okBtn.className = 'btn btn-primary px-4 py-2 rounded-3 fw-semibold';
      }
    }

    const modalInstance = bootstrap.Modal.getOrCreateInstance(modalEl);
    let isHandled = false;

    const cleanup = () => {
      modalEl.removeEventListener('hidden.bs.modal', onHidden);
    };

    const onOk = () => {
      if (isHandled) return;
      isHandled = true;
      cleanup();
      modalInstance.hide();
      resolve(true);
    };

    const onCancel = () => {
      if (isHandled) return;
      isHandled = true;
      cleanup();
      modalInstance.hide();
      resolve(false);
    };

    const onHidden = () => {
      if (isHandled) return;
      isHandled = true;
      cleanup();
      resolve(false);
    };

    okBtn.addEventListener('click', onOk);
    cancelBtn.addEventListener('click', onCancel);
    modalEl.addEventListener('hidden.bs.modal', onHidden);

    modalEl.style.setProperty('z-index', '1090', 'important');
    modalInstance.show();
  });
}

// ── Alert Modal (Thông báo giữa màn hình) ──
function showAlert(options = {}) {
  const title = typeof options === 'string' ? 'Thông báo' : (options.title || 'Thông báo');
  const message = typeof options === 'string' ? options : (options.message || '');
  const okText = options.okText || 'Đã hiểu';
  const type = options.type || 'success';
  return showConfirm({
    title,
    message,
    okText,
    type,
    hideCancel: true
  });
}

// ── Prompt Modal (Hộp thoại nhập dữ liệu ở giữa màn hình) ──
function showPrompt(options = {}) {
  return new Promise((resolve) => {
    const modalEl = document.getElementById('app-prompt-modal');
    if (!modalEl) {
      resolve(prompt(typeof options === 'string' ? options : options.message || 'Nhập giá trị:'));
      return;
    }

    const title = typeof options === 'string' ? 'Nhập thông tin' : (options.title || 'Nhập thông tin');
    const message = typeof options === 'string' ? options : (options.message || 'Vui lòng nhập thông tin bên dưới:');
    const savedUrl = localStorage.getItem('last_sheet_url') || '';
    const defaultValue = typeof options === 'object' ? (options.defaultValue || savedUrl) : savedUrl;
    const placeholder = typeof options === 'object' ? (options.placeholder || 'Nhập đường dẫn Google Sheets...') : 'Nhập đường dẫn...';

    document.getElementById('prompt-modal-title').textContent = title;
    document.getElementById('prompt-modal-message').textContent = message;

    const inputEl = document.getElementById('prompt-modal-input');
    inputEl.value = defaultValue;
    inputEl.placeholder = placeholder;

    const okBtn = document.getElementById('prompt-modal-ok-btn');
    const cancelBtn = document.getElementById('prompt-modal-cancel-btn');

    const modalInstance = bootstrap.Modal.getOrCreateInstance(modalEl);
    let isHandled = false;

    const cleanup = () => {
      okBtn.removeEventListener('click', onOk);
      cancelBtn.removeEventListener('click', onCancel);
      inputEl.removeEventListener('keydown', onKeyDown);
      modalEl.removeEventListener('hidden.bs.modal', onHidden);
    };

    const onOk = () => {
      if (isHandled) return;
      isHandled = true;
      const val = inputEl.value.trim();
      if (val && val.includes('docs.google.com/spreadsheets')) {
        localStorage.setItem('last_sheet_url', val);
      }
      cleanup();
      modalInstance.hide();
      resolve(val ? val : null);
    };

    const onCancel = () => {
      if (isHandled) return;
      isHandled = true;
      cleanup();
      modalInstance.hide();
      resolve(null);
    };

    const onHidden = () => {
      if (isHandled) return;
      isHandled = true;
      cleanup();
      resolve(null);
    };

    const onKeyDown = (e) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        onOk();
      }
    };

    okBtn.addEventListener('click', onOk);
    cancelBtn.addEventListener('click', onCancel);
    inputEl.addEventListener('keydown', onKeyDown);
    modalEl.addEventListener('hidden.bs.modal', onHidden);

    const onShown = () => {
      inputEl.focus();
      if (inputEl.value) {
        inputEl.select();
      }
      modalEl.removeEventListener('shown.bs.modal', onShown);
    };
    modalEl.addEventListener('shown.bs.modal', onShown);

    modalEl.style.setProperty('z-index', '1090', 'important');
    modalInstance.show();
  });
}

// ── Async Import Job Progress Modal ──
function showJobProgressModal(jobId, onComplete) {
  const modalEl = document.getElementById('modal-job-progress');
  if (!modalEl) {
    console.warn('modal-job-progress element not found');
    return;
  }

  const iconEl = document.getElementById('job-progress-header-icon');
  const badgeEl = document.getElementById('job-progress-status-badge');
  const barEl = document.getElementById('job-progress-bar');
  const detailEl = document.getElementById('job-progress-detail');
  const errorEl = document.getElementById('job-progress-error');
  const doneBtn = document.getElementById('job-progress-btn-done');
  const closeBtn = document.getElementById('job-progress-btn-close');

  // Reset initial UI state
  iconEl.className = 'spinner-border spinner-border-sm text-danger';
  iconEl.innerHTML = '';
  badgeEl.className = 'badge bg-primary px-3 py-2 fs-6 fw-normal mb-2';
  badgeEl.textContent = 'Đang xếp hàng đợi (PENDING)...';
  barEl.className = 'progress-bar progress-bar-striped progress-bar-animated bg-danger';
  barEl.style.width = '10%';
  barEl.textContent = '10%';
  detailEl.textContent = 'Đang chuyển tác vụ vào hàng đợi xử lý...';
  errorEl.classList.add('d-none');
  errorEl.textContent = '';
  doneBtn.classList.add('d-none');
  closeBtn.classList.add('d-none');

  const modalInstance = bootstrap.Modal.getOrCreateInstance(modalEl);
  modalInstance.show();

  let pollCount = 0;
  const maxPolls = 150; // ~ 2.5 phút
  let timerId = null;

  const poll = async () => {
    pollCount++;
    try {
      const job = await api('GET', `/api/import-jobs/${jobId}`);
      if (!job) return;

      const percent = Math.max(5, Math.min(100, job.percent || 10));
      barEl.style.width = `${percent}%`;
      barEl.textContent = `${percent}%`;

      if (job.progress) {
        detailEl.textContent = job.progress;
      }

      if (job.status === 'PROCESSING') {
        badgeEl.className = 'badge bg-warning text-dark px-3 py-2 fs-6 fw-normal mb-2';
        badgeEl.textContent = 'Đang xử lý (PROCESSING)...';
        barEl.className = 'progress-bar progress-bar-striped progress-bar-animated bg-danger';
      } else if (job.status === 'SUCCESS') {
        clearInterval(timerId);
        iconEl.className = 'bi bi-check-circle-fill text-success fs-5';
        iconEl.innerHTML = '';
        badgeEl.className = 'badge bg-success px-3 py-2 fs-6 fw-normal mb-2';
        badgeEl.textContent = 'Thành công (SUCCESS)';
        barEl.className = 'progress-bar bg-success';
        barEl.style.width = '100%';
        barEl.textContent = '100%';
        detailEl.textContent = job.progress || 'Hoàn tất nhập dữ liệu vào CSDL!';
        doneBtn.classList.remove('d-none');
        closeBtn.classList.remove('d-none');

        toast('Nhập dữ liệu thành công!', 'success');
        if (typeof onComplete === 'function') {
          setTimeout(() => onComplete(job), 1000);
        }
        return;
      } else if (job.status === 'FAILED') {
        clearInterval(timerId);
        iconEl.className = 'bi bi-x-circle-fill text-danger fs-5';
        iconEl.innerHTML = '';
        badgeEl.className = 'badge bg-danger px-3 py-2 fs-6 fw-normal mb-2';
        badgeEl.textContent = 'Thất bại (FAILED)';
        barEl.className = 'progress-bar bg-danger';
        barEl.style.width = '100%';
        barEl.textContent = 'Lỗi';
        detailEl.textContent = 'Quá trình nhập dữ liệu thất bại.';
        errorEl.textContent = job.errorMessage || 'Lỗi không xác định';
        errorEl.classList.remove('d-none');
        doneBtn.classList.remove('d-none');
        closeBtn.classList.remove('d-none');

        toast('Nhập dữ liệu thất bại: ' + (job.errorMessage || 'Lỗi'), 'error');
        return;
      }

    } catch (err) {
      console.warn('Polling error:', err);
    }

    if (pollCount >= maxPolls) {
      clearInterval(timerId);
      badgeEl.className = 'badge bg-secondary px-3 py-2 fs-6 fw-normal mb-2';
      badgeEl.textContent = 'Hết thời gian chờ';
      detailEl.textContent = 'Tác vụ vẫn đang chạy ngầm trong máy chủ. Bạn có thể kiểm tra lại sau.';
      doneBtn.classList.remove('d-none');
      closeBtn.classList.remove('d-none');
    }
  };

  // Poll ngay lập tức và sau đó mỗi 800ms
  poll();
  timerId = setInterval(poll, 800);

  modalEl.addEventListener('hidden.bs.modal', () => {
    if (timerId) clearInterval(timerId);
  }, { once: true });
}

// ── Non-blocking Background Import Job Tracker ──
function trackBackgroundImportJob(jobId, label = 'dữ liệu', onComplete) {
  if (!jobId) return;

  let pollCount = 0;
  const maxPolls = 180; // ~ 3 phút
  let timerId = null;

  const poll = async () => {
    pollCount++;
    try {
      const job = await api('GET', `/api/import-jobs/${jobId}`);
      if (!job) return;

      if (job.status === 'SUCCESS') {
        clearInterval(timerId);
        // Tác vụ import hoàn tất ngầm, tự động làm mới dữ liệu mà không cần popup thông báo
        if (typeof onComplete === 'function') {
          onComplete(job);
        }
        return;
      } else if (job.status === 'FAILED') {
        clearInterval(timerId);
        toast(`Xử lý import ${label} thất bại: ${job.errorMessage || 'Lỗi không xác định'}`, 'error', 6000);
        return;
      }
    } catch (err) {
      console.warn('Background import poll error:', err);
    }

    if (pollCount >= maxPolls) {
      clearInterval(timerId);
    }
  };

  // Poll lần đầu sau 400ms và sau đó lặp lại mỗi 1s
  setTimeout(poll, 400);
  timerId = setInterval(poll, 1000);
}

// Gắn các hàm thông báo & modal lên window object toàn cục
window.toast = toast;
window.showAlert = showAlert;
window.showConfirm = showConfirm;
window.showPrompt = showPrompt;
window.showJobProgressModal = showJobProgressModal;
window.trackBackgroundImportJob = trackBackgroundImportJob;

// Ghi đè prompt mặc định của web để hiển thị bằng Prompt Modal góc giữa màn hình
window.prompt = function (message, defaultValue = '') {
  return showPrompt({ message, defaultValue });
};

// ── Toggle password visibility ──
function togglePass(id, btn) {
  const inp = document.getElementById(id);
  const isPass = inp.type === 'password';
  inp.type = isPass ? 'text' : 'password';
  btn.innerHTML = isPass ? '<i class="bi bi-eye-slash"></i>' : '<i class="bi bi-eye"></i>';
}

// ── Clock ──
function startClock() {
  const el = document.getElementById('topbar-clock');
  const tick = () => { el.textContent = new Date().toLocaleString('vi-VN', { hour12: false }); };
  tick(); setInterval(tick, 1000);
}

// ── Page routing ──
const PAGE_TITLES = {
  dashboard: 'Dashboard',
  'manage-users': 'Quản lý thực tập sinh / Danh sách TTS',
  'admin-schedule': 'Quản lý thực tập sinh / Lịch Thực tập',
  'manage-periods': 'Quản lý thực tập sinh / Quản lý Kỳ đăng ký',
  'manage-accounts': 'Quản lý thực tập sinh / Quản lý tài khoản',
  'manage-employees': 'Quản lý nhân sự / Danh sách nhân viên',
  'manage-emp-accounts': 'Quản lý nhân sự / Quản lý tài khoản',
  'manage-ot': 'Quản lý nhân sự / Quản lý OT',
  'documents': 'Tài liệu / Quản lý tài liệu',
  'ai-config': 'Tài liệu / Cấu hình AI',
  profile: 'Cá nhân / Hồ sơ cá nhân',
  'register-ot': 'Cá nhân / Chấm công OT',
  'register-schedule': 'Cá nhân / Đăng ký lịch thực tập',
  'view-schedule': 'Cá nhân / Xem lịch của tôi',
  'change-password': 'Tài khoản / Đổi mật khẩu',
};

function navigate(page) {
  STATE.currentPage = page;
  window.currentPage = page;
  document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
  const navEl = document.getElementById('nav-' + page);
  if (navEl) navEl.classList.add('active');
  document.getElementById('topbar-title').textContent = PAGE_TITLES[page] || page;
  const area = document.getElementById('content-area');
  area.innerHTML = '<div class="d-flex justify-content-center py-5"><div class="spinner-border text-danger"></div></div>';
  renderPage(page, area);
}
window.navigate = navigate;

// ── Nav click ──
document.querySelectorAll('.nav-item[data-page]').forEach(el => {
  el.addEventListener('click', e => { e.preventDefault(); navigate(el.dataset.page); });
});

// ── Sidebar toggle ──
function setupSidebar() {
  const sidebar = document.getElementById('sidebar');
  const main = document.getElementById('main-content');
  const toggle = () => {
    if (window.innerWidth <= 768) {
      sidebar.classList.toggle('mobile-open');
    } else {
      sidebar.classList.toggle('collapsed');
      main.classList.toggle('expanded');
    }
  };
  document.getElementById('sidebar-toggle').addEventListener('click', toggle);
  document.getElementById('topbar-toggle').addEventListener('click', toggle);
}

// ── Login ──
document.getElementById('form-login').addEventListener('submit', async e => {
  e.preventDefault();
  const btn = e.target.querySelector('button[type=submit]');
  btn.querySelector('.btn-text').classList.add('d-none');
  btn.querySelector('.btn-spinner').classList.remove('d-none');
  btn.disabled = true;
  const errEl = document.getElementById('login-error');
  errEl.classList.add('d-none');
  try {
    const data = await api('POST', '/auth/login', {
      username: document.getElementById('login-code').value.trim(),
      password: document.getElementById('login-pass').value,
    });
    saveSession(data);
    showApp();
  } catch (err) {
    errEl.textContent = err.message;
    errEl.classList.remove('d-none');
  } finally {
    btn.querySelector('.btn-text').classList.remove('d-none');
    btn.querySelector('.btn-spinner').classList.add('d-none');
    btn.disabled = false;
  }
});

// ── Logout ──
document.getElementById('nav-logout').addEventListener('click', e => {
  e.preventDefault();
  if (typeof destroyChat === 'function') destroyChat();
  clearSession();
  document.getElementById('page-app').classList.remove('active');
  document.getElementById('page-login').classList.add('active');
});

// ── Show app after login ──
function showApp() {
  document.getElementById('page-login').classList.remove('active');
  const appEl = document.getElementById('page-app');
  appEl.classList.add('active');
  document.getElementById('sidebar-name').textContent = STATE.fullName;

  const isAdmin = STATE.role === 'admin';
  const isEmployee = STATE.user_type === 'employee';
  const isIntern = !isAdmin && !isEmployee;

  // Role label
  let roleLabel = 'Thực tập sinh';
  if (isAdmin) roleLabel = 'Admin';
  else if (isEmployee) roleLabel = 'Nhân viên';
  document.getElementById('sidebar-role').textContent = roleLabel;

  // Menus visibility
  document.getElementById('admin-menu').classList.toggle('d-none', !isAdmin);
  document.getElementById('ai-menu').classList.toggle('d-none', !isAdmin);
  document.getElementById('employee-menu').classList.toggle('d-none', !isEmployee);

  // Nav đến trang sau login
  setupSidebar();
  startClock();
  if (typeof initChat === 'function') initChat();

  if (isAdmin) navigate('dashboard');
  else navigate('profile');
}

// ── Init ──
window.addEventListener('DOMContentLoaded', () => {
  if (loadSession()) showApp();
});
