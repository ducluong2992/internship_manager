@echo off
echo ============================================
echo   VIETTEL INTERN MANAGEMENT SYSTEM
echo ============================================
echo.

cd /d "%~dp0backend"

echo [1/2] Checking Python environment...

set "PYTHON_CMD=python"
%PYTHON_CMD% --version >nul 2>&1
if errorlevel 1 (
    set "PYTHON_CMD=py"
    py --version >nul 2>&1
)
if errorlevel 1 (
    set "PYTHON_CMD=C:\Users\HCL\AppData\Local\Programs\Python\Python310\python.exe"
    "%PYTHON_CMD%" --version >nul 2>&1
)
if errorlevel 1 (
    echo [ERROR] Python is not installed or not found!
    pause & exit /b 1
)

echo [OK] Using Python: %PYTHON_CMD%

echo [2/2] Checking libraries...
"%PYTHON_CMD%" -m pip show fastapi >nul 2>&1
if errorlevel 1 (
    echo Installing libraries from requirements.txt...
    "%PYTHON_CMD%" -m pip install -r requirements.txt
)

echo.
echo [OK] Server starting at http://localhost:8000
echo [OK] Default account: admin / Admin@123
echo.
echo Press Ctrl+C to stop the server
echo.
"%PYTHON_CMD%" main.py
pause

