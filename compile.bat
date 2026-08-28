@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo [FastMessaging] Compiling and Testing...
call mvn clean test
pause
