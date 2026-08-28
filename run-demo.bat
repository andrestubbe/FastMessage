@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo [FastMessaging] Running Hero Demo...
call mvn compile exec:java -Dexec.mainClass=fastmessaging.Demo
pause
