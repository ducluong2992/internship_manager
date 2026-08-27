@echo off
chcp 65001 >nul
echo ============================================
echo   DỪNG MÁY CHỦ QUẢN LÝ NHÂN SỰ
echo ============================================
echo.

echo Đang tìm và tắt tiến trình trên cổng 8000...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":8000" ^| findstr "LISTENING"') do (
    echo Đang dừng tiến trình PID: %%a
    taskkill /F /PID %%a >nul 2>&1
)

echo.
echo [OK] Máy chủ đã dừng thành công! Cổng 8000 đã được giải phóng.
echo.
pause
