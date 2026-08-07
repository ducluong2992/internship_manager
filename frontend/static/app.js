/* ═══════════════════════════════════════════════
   VIETTEL INTERN & EMPLOYEE MANAGEMENT — FRONTEND
   ═══════════════════════════════════════════════ */

const API = 'http://localhost:8000';
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
    const type = options.type || 'danger'; // 'danger', 'warning', 'info'

    document.getElementById('confirm-modal-title').textContent = title;
    document.getElementById('confirm-modal-message').textContent = message;
    
    const okBtn = document.getElementById('confirm-modal-ok-btn');
    const cancelBtn = document.getElementById('confirm-modal-cancel-btn');
    const iconBg = document.getElementById('confirm-modal-icon-bg');
    const iconEl = document.getElementById('confirm-modal-icon');

    okBtn.textContent = okText;
    cancelBtn.textContent = cancelText;

    // Config Icon & Colors based on type
    if (iconBg && iconEl) {
      iconBg.className = 'confirm-icon-wrap mx-auto mb-3 confirm-icon-' + type;
      if (type === 'danger') {
        iconEl.className = 'bi bi-trash3-fill';
        okBtn.className = 'btn btn-danger px-4 py-2 rounded-3 fw-semibold';
      } else if (type === 'warning') {
        iconEl.className = 'bi bi-exclamation-triangle-fill';
        okBtn.className = 'btn btn-warning text-white px-4 py-2 rounded-3 fw-semibold';
      } else {
        iconEl.className = 'bi bi-question-circle-fill';
        okBtn.className = 'btn btn-primary px-4 py-2 rounded-3 fw-semibold';
      }
    }

    const modalInstance = bootstrap.Modal.getOrCreateInstance(modalEl);

    let isHandled = false;

    const cleanup = () => {
      okBtn.removeEventListener('click', onOk);
      cancelBtn.removeEventListener('click', onCancel);
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
    const defaultValue = typeof options === 'object' ? (options.defaultValue || '') : '';
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

    modalEl.addEventListener('shown.bs.modal', () => {
      inputEl.focus();
      inputEl.select();
    }, { once: true });

    modalEl.style.setProperty('z-index', '1090', 'important');
    modalInstance.show();
  });
}

// Gắn các hàm thông báo & modal lên window object toàn cục
window.toast = toast;
window.showConfirm = showConfirm;
window.showPrompt = showPrompt;

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
  document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
  const navEl = document.getElementById('nav-' + page);
  if (navEl) navEl.classList.add('active');
  document.getElementById('topbar-title').textContent = PAGE_TITLES[page] || page;
  const area = document.getElementById('content-area');
  area.innerHTML = '<div class="d-flex justify-content-center py-5"><div class="spinner-border text-danger"></div></div>';
  renderPage(page, area);
}

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
