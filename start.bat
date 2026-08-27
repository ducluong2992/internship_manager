@echo off
chcp 65001 >nul
echo ===================================================
echo   HỆ THỐNG QUẢN LÝ NHÂN SỰ & THỰC TẬP SINH
echo   SPRING BOOT 3 (JAVA 17) ENTERPRISE
echo ===================================================
echo.

cd /d "%~dp0backend"

echo [1/2] Kiểm tra môi trường Java 17+...
set "JAVA_CMD=java"
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
)

"%JAVA_CMD%" -version >nul 2>&1
if errorlevel 1 (
    if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot\bin\java.exe" (
        set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot"
        set "JAVA_CMD=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot\bin\java.exe"
    ) else (
        echo [ERROR] Không tìm thấy Java 17+. Vui lòng cài đặt JDK 17+ hoặc cấu hình biến môi trường JAVA_HOME!
        pause & exit /b 1
    )
)

echo [OK] Môi trường Java sẵn sàng.
echo [2/2] Khởi chạy máy chủ Backend & Frontend...
echo.
echo ===================================================
echo [OK] Ứng dụng chạy tại: http://localhost:8000
echo [OK] Tài khoản mặc định: admin / Admin@123
echo ===================================================
echo.
echo Nhấn Ctrl+C để dừng máy chủ.
echo.

if exist "target\qlnv-backend-2.0.0.jar" (
    "%JAVA_CMD%" -jar target\qlnv-backend-2.0.0.jar
) else (
    echo Đang biên dịch và chạy dự án qua Maven...
    where mvn >nul 2>&1
    if not errorlevel 1 (
        mvn spring-boot:run
    ) else if exist "C:\tools\apache-maven-3.9.9\bin\mvn.cmd" (
        call "C:\tools\apache-maven-3.9.9\bin\mvn.cmd" spring-boot:run
    ) else (
        echo [ERROR] Không tìm thấy Maven (mvn). Vui lòng cài đặt Maven hoặc chạy 'mvn package' trước!
        pause & exit /b 1
    )
)
pause

