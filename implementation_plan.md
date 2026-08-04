# Nâng cấp: Hệ thống Quản lý Nhân sự (Employee Module)

## Tổng quan

Bổ sung module **Quản lý Nhân sự** vào hệ thống hiện có (QuanLiTTS), tái sử dụng bảng `users` và hệ thống tài khoản chung, chỉ thêm các trường và chức năng mới phục vụ quản lý nhân viên chính thức. Không phá vỡ cấu trúc hiện có của module Thực tập sinh.

---

## User Review Required

> [!IMPORTANT]
> **Thay đổi role hệ thống:** Hệ thống hiện có 2 role `admin` / `intern`. Sau khi nâng cấp chỉ dùng 2 role: `admin` / `user`. Tất cả intern và employee đều mang role `user`, phân biệt nhau bằng cột `user_type` (`intern` / `employee`). Cần **migration** cập nhật các bản ghi `role = 'intern'` → `role = 'user'` trong DB.

> [!NOTE]
> **Export Google Sheet:** Chức năng "Xuất Google Sheet" giữ nguyên cách tiếp cận hiện có: xuất `.xlsx`, import từ link GSheet public.

---

## Proposed Changes

---

### Backend – Database

#### [MODIFY] [models.py](file:///d:/Agent_Tutor/QuanLiTTS/backend/models.py)

**Bảng `users` – bổ sung cột:**

| Cột | Kiểu | Mô tả | Default |
|-----|------|-------|---------|
| `user_type` | String | `intern` / `employee` | `intern` |
| `position_id` | Integer (FK→positions) | Vị trí công việc | NULL |
| `computer_serial` | String | Seri máy tính | NULL |
| `employment_status` | String | Thử việc / Chính thức | `Thử việc` |
| `use_company_mac` | String | Có / Không | `Không` |
| `staff_category` | String | NS trung tâm / Onsite / Cho mượn | `NS trung tâm` |
| `seat_position` | String | Vị trí ngồi | NULL |
| `direct_manager` | String | Username quản lý trực tiếp | NULL |
| `borrow_end_date` | Date | Thời hạn mượn | NULL |
| `borrow_project` | String | Dự án mượn | NULL |
| `borrow_pm` | String | Username PM dự án mượn | NULL |
| `borrow_center` | String | Trung tâm cho mượn | NULL |

**⚠ Migration:** Cột `role` chỉ còn 2 giá trị: `admin` / `user`. Mọi `role = 'intern'` → `role = 'user'`.

**Bảng `positions` – MỚI:**

| Cột | Kiểu | Mô tả |
|-----|------|-------|
| `id` | Integer PK | |
| `name` | String unique | Tên vị trí |
| `is_manager` | Boolean | True nếu có thể là quản lý trực tiếp |

**Dữ liệu seed `positions`:**

| Tên | is_manager |
|-----|-----------|
| Trợ lý dự án | False |
| PM | **True** |
| DU Lead | **True** |
| GDTT | **True** |
| PGDTT | **True** |
| Dev | False |
| Dev Lead | **True** |
| Dev Mobile | False |
| DevOps | False |
| Tester | False |
| Test Lead | **True** |
| BA | False |
| BA Lead | **True** |
| QA | False |
| DA | False |
| AI | False |

---

#### [NEW] [routers/employee_router.py](file:///d:/Agent_Tutor/QuanLiTTS/backend/routers/employee_router.py)

Router `/employees` với các API:

| Method | Path | Mô tả |
|--------|------|-------|
| GET | `/employees` | Danh sách nhân viên (`user_type=employee`) |
| POST | `/employees` | Thêm nhân viên + tạo tài khoản |
| PUT | `/employees/{id}` | Cập nhật nhân viên |
| DELETE | `/employees/{id}` | Xóa nhân viên |
| GET | `/employees/managers` | Users có `position.is_manager = True` |
| GET | `/employees/positions` | Danh sách tất cả vị trí |
| GET | `/employees/import-template` | Tải mẫu Excel |
| POST | `/employees/import` | Import từ Excel |
| POST | `/employees/import-link` | Import từ Google Sheets link |
| GET | `/employees/export` | Xuất Excel |

**Logic sinh username** (prefix `nv_`):
```
"Trần Đức Lương" → remove_accents → "Tran Duc Luong"
→ last_word = "luong", initials = "td"
→ base = "nv_luongtd"
→ nếu trùng: "nv_luongtd1", "nv_luongtd2", ...
```

**Mật khẩu mặc định:** `123456` (bcrypt hash).

**Endpoint `/employees/managers`** trả về:
```json
[{ "id": 1, "full_name": "Nguyễn Văn A", "username": "nguyenvana", "position": "PM" }]
```
→ Dropdown hiển thị `full_name`, lưu `username` (phần trước `@` của email, hoặc username tài khoản).

---

#### [MODIFY] [schemas.py](file:///d:/Agent_Tutor/QuanLiTTS/backend/schemas.py)

- Thêm `PositionResponse` schema
- Bổ sung các trường mới vào `UserBase`, `UserUpdate`, `UserResponse`
- Thêm `EmployeeCreate` với defaults: `role="user"`, `user_type="employee"`, `bank_name="Viettel Money"`, `employment_status="Thử việc"`, `staff_category="NS trung tâm"`
- Thêm `EmployeeImportResult` (success_count, skip_count, renamed_usernames[])

---

#### [MODIFY] [auth.py](file:///d:/Agent_Tutor/QuanLiTTS/backend/auth.py)

- `require_admin`: giữ nguyên, kiểm tra `role == 'admin'`
- Không cần thêm middleware mới — role `user` bao gồm cả intern và employee, phân biệt bởi `user_type`

---

#### [MODIFY] [main.py](file:///d:/Agent_Tutor/QuanLiTTS/backend/main.py)

- Đăng ký `employee_router`
- Bổ sung hàm `seed_positions()` để seed bảng `positions` khi khởi động

---

### Backend – Migration cần thiết

> [!WARNING]
> Khi khởi động lần đầu sau nâng cấp, cần chạy migration SQL:
> ```sql
> UPDATE users SET role = 'user' WHERE role = 'intern';
> ```
> Sẽ được tích hợp vào hàm `seed_admin()` trong `main.py` để tự động thực hiện.

---

### Frontend – index.html

#### [MODIFY] [index.html](file:///d:/Agent_Tutor/QuanLiTTS/frontend/index.html)

- Cập nhật menu `admin-menu`: thêm item **Quản lý Nhân sự** (đã có placeholder)
- Thêm **`user-menu`** (thay `intern-menu`): role `user` với `user_type=intern` thấy lịch, `user_type=employee` không thấy lịch — xử lý bằng JS
- Thêm **Modal nhân viên** (`modal-employee`):

```
─ Thông tin cơ bản ─────────────────────────────
[Họ và tên *]          [Mã nhân viên *]
[Vị trí (dropdown)]    [Dự án]
[Quản lý trực tiếp (dropdown is_manager=True)]

─ Thông tin cá nhân ────────────────────────────
[Giới tính]  [Dân tộc]  [Email Viettel]
[Ngày sinh]  [Quê quán] [Số điện thoại]
[CCCD]       [Seri máy tính]

─ Tài chính ────────────────────────────────────
[Số tài khoản]   [Ngân hàng = "Viettel Money"]

─ Công việc ────────────────────────────────────
[Tình trạng: Thử việc/Chính thức]
[Dùng Mac công ty: Có/Không]
[Vị trí ngồi]
[Loại nhân sự: NS trung tâm / Onsite / Cho mượn]

─ (Chỉ hiện khi Cho mượn) ──────────────────────
[Thời hạn mượn]  [Dự án mượn]
[PM phụ trách (dropdown is_manager=True)]
[Trung tâm cho mượn]
```

- Thêm `<script src="static/page_employees.js">` vào cuối body

---

### Frontend – app.js

#### [MODIFY] [app.js](file:///d:/Agent_Tutor/QuanLiTTS/frontend/static/app.js)

- Cập nhật `PAGE_TITLES` thêm `'manage-employees'`
- Lưu `user_type` vào `STATE` khi đăng nhập
- Cập nhật `showApp()`:
  - `admin` → thấy `admin-menu`, điều hướng `dashboard`
  - `user` → thấy `user-menu`, điều hướng `profile`
    - Nếu `user_type = 'intern'`: hiện thêm mục Đăng ký lịch + Xem lịch
    - Nếu `user_type = 'employee'`: ẩn mục lịch
- Cập nhật `sidebar-role`: "Nhân viên" / "Thực tập sinh"

> [!NOTE]
> API `/auth/login` cần trả về thêm trường `user_type` trong `TokenResponse`.

---

### Frontend – pages.js

#### [MODIFY] [pages.js](file:///d:/Agent_Tutor/QuanLiTTS/frontend/static/pages.js)

- Thêm `case 'manage-employees': await renderManageEmployees(area); break;`
- Cập nhật `renderDashboard()`: thêm stat card "Tổng nhân viên"

---

### Frontend – page_employees.js (MỚI)

#### [NEW] [page_employees.js](file:///d:/Agent_Tutor/QuanLiTTS/frontend/static/page_employees.js)

Toàn bộ logic trang Quản lý Nhân sự:

| Hàm | Mô tả |
|-----|-------|
| `renderManageEmployees(area)` | Render bảng + filter |
| `openEmployeeModal(emp)` | Mở modal thêm/sửa, load dropdown positions + managers |
| `handleStaffCategoryChange()` | Enable/disable các trường borrow |
| `saveEmployee()` | POST/PUT API |
| `deleteEmployee(id, name)` | DELETE API |
| `downloadEmployeeTemplate()` | Tải mẫu Excel |
| `handleEmployeeImportExcel(e)` | Import từ file xlsx |
| `promptEmployeeImportLink()` | Import từ Google Sheets link |
| `exportEmployees()` | Xuất Excel danh sách |

**Import Excel – kết quả hiển thị:**
```
✅ Thành công: 5 nhân viên
⚠️ Đổi username (do trùng): nv_luongtd → nv_luongtd1
❌ Bỏ qua (trùng mã NV): 2 người
```

---

### Frontend – page_profile.js

#### [MODIFY] [page_profile.js](file:///d:/Agent_Tutor/QuanLiTTS/frontend/static/page_profile.js)

- Khi `STATE.user_type === 'employee'`: hiển thị thêm các trường nhân viên (employment_status, staff_category, v.v.)
- Không hiển thị phần lịch

---

### Frontend – page_users.js

#### [MODIFY] [page_users.js](file:///d:/Agent_Tutor/QuanLiTTS/frontend/static/page_users.js)

- Cập nhật filter: lọc `user_type === 'intern'` thay vì `role === 'intern'`
- Modal form TTS: trường Vai trò chỉ còn `user` / `admin`

---

## Phân quyền tổng thể

| Màn hình | admin | user (intern) | user (employee) |
|----------|-------|---------------|-----------------|
| Dashboard | ✅ | ❌ | ❌ |
| Quản lý TTS | ✅ | ❌ | ❌ |
| Quản lý Nhân sự | ✅ | ❌ | ❌ |
| Kỳ đăng ký | ✅ | ❌ | ❌ |
| Tài khoản | ✅ | ❌ | ❌ |
| Hồ sơ cá nhân | ✅ | ✅ | ✅ |
| Đăng ký lịch | ❌ | ✅ | ❌ |
| Xem lịch | ❌ | ✅ | ❌ |
| Đổi mật khẩu | ✅ | ✅ | ✅ |

---

## Verification Plan

### Automated Tests
- Backend khởi động không lỗi: `uvicorn main:app --reload`
- `GET /employees` → 200 (với admin token)
- `GET /employees/managers` → list users có position `is_manager=True`
- `GET /employees/positions` → 16 positions
- Username generation: "Trần Đức Lương" → `nv_luongtd`

### Manual Verification
1. ✅ Admin đăng nhập → thấy menu "Quản lý Nhân sự"
2. ✅ Thêm nhân viên → tài khoản tự tạo `nv_*`, mật khẩu `123456`
3. ✅ Dropdown "Quản lý trực tiếp" → chỉ hiện users có position `is_manager=True`
4. ✅ Dropdown "Vị trí" → 16 vị trí từ bảng `positions`
5. ✅ Chọn "Cho mượn" → trường borrow enable; chọn khác → disable + clear
6. ✅ Import Excel → hiển thị chi tiết: thành công / bỏ qua / đổi username
7. ✅ Đăng nhập tài khoản employee → chỉ thấy Hồ sơ + Đổi mật khẩu
8. ✅ Đăng nhập tài khoản intern → thấy Hồ sơ + Đăng ký lịch + Xem lịch
9. ✅ Xuất Excel danh sách nhân viên
