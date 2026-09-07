@echo off
chcp 65001 >nul
echo ============================================
echo   DUNG MAY CHU QUAN LY NHAN SU
echo ============================================
echo.

echo Dang tim va tat tien trinh tren cong 8088...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":8088" ^| findstr "LISTENING"') do (
    echo Dang dung tien trinh PID: %%a
    taskkill /F /PID %%a >nul 2>&1
)

echo.
echo [OK] May chu da dung thanh cong! Cong 8088 da duoc giai phong.
echo.
pause
