@echo off
echo ============================================
echo   Hotel Booking Server - Spring Boot
echo ============================================
echo.
cd /d %~dp0
mvnw.cmd spring-boot:run
pause