# 📌 HƯỚNG DẪN ĐỒNG BỘ GIT - NHÁNH `java` CHO 3 THÀNH VIÊN NHÓM

Tài liệu này được biên soạn để **3 bạn trong nhóm (Minh, Lương, Nghĩa)** nắm rõ cấu trúc nhánh, tránh bị đè code (conflict/overwrite) hoặc mất dữ liệu khi cùng phát triển dự án.

---

## 🎯 1. Nhánh `java` chứa những gì?
Nhánh **`java`** là nhánh **Backend Java Spring Boot 3** hoàn chỉnh thay thế toàn bộ mã nguồn Python cũ:
1. **Backend**: Spring Boot 3.2.3, JDK 17, Maven, SQLite, Spring Security + JWT.
2. **Nghiệp vụ đã hoàn thiện**:
   - Quản lý Nhân sự & Thực tập sinh (CRUD, Import/Export Excel).
   - Đăng ký & Tính giờ làm thêm Overtime (OT 24h, tính ca đêm/ngày, trừ giờ hành chính, phân tách đa ngày).
   - Lịch làm việc & Phân ca (Schedule Periods).
   - Tài liệu nghiệp vụ & Trích xuất tri thức cho Chatbot AI (Gemini RAG).
3. **Frontend**: Đã đồng bộ toàn bộ endpoint về tiền tố `/api/...`, tương thích 100% với Spring Boot.
4. **Script khởi động**: `start.bat` (1-click chạy cổng `8088`), `stop.bat` (tắt và giải phóng port).

---

## ⚠️ 2. Quy Tắc Vàng Khi Làm Việc Nhóm (Tránh Lỗi Git)

> [!IMPORTANT]
> **Tuyệt đối tuân thủ 4 quy tắc sau để không làm hỏng code của nhau:**

1. **Không force push (`git push -f`)** lên nhánh `main` hoặc `java`.
2. **Không code trực tiếp đè lên nhánh của bạn khác** (`branchLuong`, `nenghia`, `OT`).
3. **Trước khi bắt đầu code**: Luôn chạy `git fetch` và `git pull` để nhận code mới nhất.
4. **Mỗi người khi làm tính năng mới**: Nên tạo một nhánh riêng từ `java` (ví dụ: `git checkout -b feature-cua-minh java`), sau khi làm xong thì push nhánh đó lên và tạo Pull Request (PR) để cả nhóm review gộp vào.

---

## 📥 3. Hướng Dẫn Kéo Code Nhánh `java` Về Máy Của Từng Bạn

### Bước 1: Kiểm tra remote Git trên máy
Mở Terminal/PowerShell tại thư mục dự án và gõ:
```bash
git remote -v
```

Nếu chưa có remote `luong` trỏ về repo chứa nhánh `java`, hãy thêm vào:
```bash
git remote add luong https://github.com/ducluong2992/internship_manager.git
```

### Bước 2: Kéo toàn bộ thông tin nhánh mới nhất
```bash
git fetch luong
```

### Bước 3: Chuyển sang nhánh `java`
```bash
# Nếu máy bạn chưa từng có nhánh java:
git checkout -b java luong/java

# Nếu máy bạn đã có nhánh java:
git checkout java
git pull luong java
```

### Bước 4: Kiểm tra trạng thái
```bash
git status
```
*(Kết quả thông báo: `Your branch is up to date with 'luong/java'` và `working tree clean` là chuẩn).*

---

## 🚀 4. Hướng Dẫn Đẩy Code Mới Lên (Khi Bạn Làm Thêm Tính Năng)

### Cách an toàn nhất (Khuyến nghị):
```bash
# 1. Đang ở nhánh java, tạo nhánh mới của bạn
git checkout -b feat/ten-tinh-nang

# 2. Thực hiện sửa đổi code và kiểm tra
git status

# 3. Thêm các file đã thay đổi và commit ghi chú rõ ràng
git add .
git commit -m "feat(module-name): mô tả chi tiết việc bạn vừa làm"

# 4. Đẩy nhánh của bạn lên GitHub
git push luong feat/ten-tinh-nang
```
Sau đó lên GitHub tạo Pull Request gộp vào nhánh `java` để cả 3 bạn cùng biết và thống nhất.

---

## 📋 5. Danh Sách Các Commit Quan Trọng Đã Có Trên Nhánh `java`

| Mã Commit | Tác giả | Nội dung công việc |
| :--- | :--- | :--- |
| `60afa38` | AIREAD | `fix: dong bo route /api/ cho documents, ai-config va chat, trich xuat tri thuc tai lieu vao Gemini RAG` |
| `33636c3` | AIREAD | `fix: cap nhat API lich lam viec, sua thong ke dashboard, sua start.bat CRLF va ho tro port 8088` |
| `6bdded0` | AIREAD | `feat: chuyen doi toan bo backend sang Java Spring Boot 3 va bo sung huong dan cai dat` |
| `ddf1ed1` | AIREAD | `feat(ot): Hoàn thiện tính OT liên tục từ ngày này sang ngày khác, tự động trừ giờ hành chính` |
| `4c4a34e` | AIREAD | `fix(ot): Đăng ký từ 18 đến 19 cho ca qua đêm là 1 ca 12 tiếng duy nhất` |

---

## ❓ 6. Lưu ý về Remote `origin` (PhungBuiNgocMinh)
Hiện tại khi fetch hoặc push vào `origin` (`https://github.com/PhungBuiNgocMinh/internship_manager.git`), nếu nhận thông báo:
```text
remote: Repository not found.
fatal: repository 'https://github.com/PhungBuiNgocMinh/internship_manager.git/' not found
```
**Nguyên nhân:** Repo của bạn Minh đang được cài đặt ở chế độ **Private**, tài khoản Git trên máy hiện tại chưa được cấp quyền Collaborator hoặc Token/SSH chưa có quyền truy cập repo này.
**Giải pháp:** 
- Minh vào GitHub repo -> `Settings` -> `Collaborators` -> Invite 2 bạn còn lại.
- Hoặc cả 3 bạn thống nhất sử dụng chung repo `https://github.com/ducluong2992/internship_manager.git` làm nguồn chuẩn để push/pull chung.
