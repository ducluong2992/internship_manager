@echo off
echo ============================================
echo   VIETTEL INTERN MANAGEMENT SYSTEM
echo ============================================
echo.

cd /d "%~dp0backend"

echo [1/2] Checking Python environment...
python --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Python is not installed!
    pause & exit /b 1
)

echo [2/2] Checking libraries...
pip show fastapi >nul 2>&1
if errorlevel 1 (
    echo Installing libraries...
    pip install -r requirements.txt
)

echo.
echo [OK] Server starting at http://localhost:8000
echo [OK] Default account: admin / Admin@123
echo.
echo Press Ctrl+C to stop the server
echo.
python main.py
pause
