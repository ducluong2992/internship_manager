# 🚀 HƯỚNG DẪN CÀI ĐẶT VÀ CHẠY DỰ ÁN QUẢN LÝ NHÂN SỰ & THỰC TẬP SINH (JAVA SPRING BOOT 3)

Tài liệu này hướng dẫn chi tiết cách cài đặt công nghệ, thiết lập môi trường và các bước khởi động/tắt toàn bộ hệ thống từ A-Z.

---

## 📌 1. Yêu cầu công nghệ & Phần mềm cần cài đặt

Để chạy dự án, máy tính của bạn cần cài đặt 2 công cụ cốt lõi sau:

### 1.1. Java Development Kit (JDK 17 trở lên)
* **Phiên bản khuyến nghị:** Eclipse Adoptium Temurin JDK 17 hoặc OpenJDK 17/21.
* **Tải tại:** [https://adoptium.net/temurin/releases/?version=17](https://adoptium.net/temurin/releases/?version=17)
* **Kiểm tra sau khi cài:** Mở Terminal / PowerShell và gõ:
  ```bash
  java -version
  ```
  *(Kết quả hiển thị `openjdk version "17.x.x"` hoặc tương đương là thành công)*.

### 1.2. Apache Maven (Công cụ build Java)
* **Phiên bản khuyến nghị:** Maven 3.8.x hoặc 3.9.x.
* **Tải tại:** [https://maven.apache.org/download.cgi](https://maven.apache.org/download.cgi)
* **Kiểm tra sau khi cài:**
  ```bash
  mvn -v
  ```

---

## 💻 2. Cách khởi chạy chương trình

Bạn có thể chọn 1 trong 2 cách sau:

### 🌟 CÁCH 1: Dùng File Tự Động (Khuyến nghị - Nhanh nhất)
1. Trong thư mục gốc của dự án, **nhấp đúp chuột (Double Click) vào file `start.bat`**.
2. Hệ thống sẽ tự động:
   - Kiểm tra JDK 17 & Maven.
   - Biên dịch và khởi động máy chủ Spring Boot.
   - Host toàn bộ Frontend và API Backend trên cùng cổng `8000`.
3. Mở trình duyệt web và truy cập địa chỉ:
   👉 **`http://localhost:8000`**

*(Nếu muốn chạy ngầm ẩn cửa sổ đen: Nhấp đúp chuột vào `start_hidden.vbs`)*.

---

### 🛠 CÁCH 2: Khởi chạy bằng dòng lệnh (Terminal / CMD / PowerShell)
1. Mở Terminal tại thư mục dự án và chuyển vào thư mục `backend`:
   ```bash
   cd backend
   ```
2. Chạy ứng dụng với Maven:
   ```bash
   mvn spring-boot:run
   ```
   *(Hoặc nếu đã build ra file jar: `mvn clean package` sau đó chạy `java -jar target/qlnv-backend-2.0.0.jar`)*.
3. Mở trình duyệt web và truy cập: **`http://localhost:8000`**.

---

## 🛑 3. Cách tắt chương trình
Khi không sử dụng nữa hoặc cần khởi động lại:
* **Cách 1:** Nhấp đúp chuột vào file **`stop.bat`** (Tự động tìm và giải phóng cổng 8000).
* **Cách 2:** Nhấn tổ hợp phím **`Ctrl + C`** trên cửa sổ console đang chạy `start.bat`.

---

## 🔑 4. Tài khoản đăng nhập hệ thống

| Vai trò | Tên đăng nhập (Username) | Mật khẩu (Password) |
| :--- | :--- | :--- |
| **Quản trị viên (Admin)** | `admin` | `Admin@123` |

---

## 📂 5. Cấu trúc thư mục dự án

```text
qlnv1/
├── backend/                  # Mã nguồn Backend Java Spring Boot 3
│   ├── pom.xml               # Cấu hình thư viện Maven & Dependencies
│   ├── src/main/java/        # Code xử lý nghiệp vụ, API RESTful, Bảo mật JWT
│   └── src/main/resources/   # File application.yml cấu hình hệ thống & SQLite
├── frontend/                 # Giao diện người dùng Web SPA (HTML5, CSS3, JS ES6+)
│   ├── index.html            # Trang giao diện chính
│   └── static/               # CSS, JavaScript xử lý từng phân hệ (OT, Nhân sự, Lịch...)
├── start.bat                 # Script 1-click khởi động hệ thống
├── stop.bat                  # Script 1-click tắt máy chủ & giải phóng cổng
├── start_hidden.vbs          # Script khởi động ngầm không hiện cửa sổ
├── .gitignore                # Danh sách file loại trừ không đẩy lên Git
└── HUONG_DAN_CAI_DAT_VA_CHAY.md # Hướng dẫn chi tiết này
```

---

## ⚙️ 6. Lưu ý quan trọng
* Toàn bộ hệ thống cũ chạy bằng **Python (FastAPI)** đã được dọn dẹp và chuyển đổi 100% sang kiến trúc **Java Spring Boot Enterprise**.
* Dữ liệu SQLite được tự động quản lý và đồng bộ, không cần cài đặt thêm phần mềm cơ sở dữ liệu bên ngoài.
