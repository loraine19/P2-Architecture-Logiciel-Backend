@echo off
REM Script to start the Spring Boot development server with automatic restart
REM Usage: server.bat [start|stop|restart|status]

if "%1"=="" goto start
if "%1"=="start" goto start
if "%1"=="stop" goto stop
if "%1"=="restart" goto restart
if "%1"=="status" goto status
goto help

:start
echo 🚀 Starting Spring Boot development server with auto-restart...
echo 📝 DevTools is enabled - changes will trigger automatic restart
echo 🔗 Server will be available at: http://localhost:8080
echo ⏹️  Press Ctrl+C to stop
echo.
mvn clean spring-boot:run
goto end

:stop
echo ⏹️  Stopping Spring Boot server...
taskkill /f /im java.exe 2>nul || echo No Java processes found
goto end

:restart
echo 🔄 Restarting Spring Boot server...
call %0 stop
timeout /t 2 /nobreak >nul
call %0 start
goto end

:status
tasklist /fi "imagename eq java.exe" 2>nul | find /i "java.exe" >nul
if %errorlevel%==0 (
    echo ✅ Spring Boot server is running
) else (
    echo ❌ Spring Boot server is not running
)
goto end

:help
echo Usage: %0 [start^|stop^|restart^|status]
echo.
echo Commands:
echo   start    - Start the development server (default)
echo   stop     - Stop the server
echo   restart  - Restart the server
echo   status   - Check if server is running
echo.
echo Examples:
echo   server.bat          # Start server
echo   server.bat start    # Start server
echo   server.bat stop     # Stop server

:end