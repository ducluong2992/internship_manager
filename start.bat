@echo off
chcp 65001 >nul
echo ===================================================
echo   HE THONG QUAN LY NHAN SU VA THUC TAP SINH
echo   SPRING BOOT 3 (JAVA 17) ENTERPRISE
echo ===================================================
echo.

cd /d "%~dp0backend"

echo [1/2] Kiem tra moi truong Java 17+...
set "JAVA_CMD=java"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"

"%JAVA_CMD%" -version >nul 2>&1
if errorlevel 1 (
    for /d %%D in ("C:\Program Files\Eclipse Adoptium\jdk-17*" "C:\Program Files\Java\jdk-17*" "C:\Program Files\Microsoft\jdk-17*") do (
        if exist "%%D\bin\java.exe" (
            set "JAVA_CMD=%%D\bin\java.exe"
            set "JAVA_HOME=%%D"
            set "PATH=%%D\bin;%PATH%"
        )
    )
)

"%JAVA_CMD%" -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Khong tim thay Java 17+. Vui long cai dat JDK 17+ hoac cau hinh JAVA_HOME!
    pause
    exit /b 1
)

echo [OK] Moi truong Java san sang.
echo [2/2] Khoi chay may chu Backend va Frontend...
echo.
echo ===================================================
echo [OK] Ung dung chay tai: http://localhost:8088
echo [OK] Tai khoan mac dinh: admin / Admin@123
echo ===================================================
echo.
echo Nhan Ctrl+C de dung may chu.
echo.

if exist "target\qlnv-backend-2.0.0.jar" goto RUN_JAR

where mvn >nul 2>&1
if not errorlevel 1 goto RUN_MVN

if exist "C:\tools\apache-maven-3.9.9\bin\mvn.cmd" goto RUN_CUSTOM_MVN

echo [ERROR] Khong tim thay Maven hoac file jar da build!
pause
exit /b 1

:RUN_JAR
"%JAVA_CMD%" -jar target\qlnv-backend-2.0.0.jar --server.port=8088
goto END

:RUN_MVN
mvn spring-boot:run
goto END

:RUN_CUSTOM_MVN
call "C:\tools\apache-maven-3.9.9\bin\mvn.cmd" spring-boot:run
goto END

:END
pause
