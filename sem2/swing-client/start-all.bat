@echo off
echo ============================================
echo   Hotel Booking System - Full Startup
echo ============================================
echo.

echo [1/2] Starting Spring Boot Server...
start cmd /k "cd /d demo && mvnw.cmd spring-boot:run"

echo Waiting for server to start...
timeout /t 10 /nobreak >nul

echo [2/2] Starting Swing Client...
start cmd /k "cd /d swing-client && run.bat"

echo.
echo ============================================
echo   SYSTEM READY!
echo   Web:      http://localhost:8080
echo   Swing:    Running in separate window
echo ============================================
echo.
pause