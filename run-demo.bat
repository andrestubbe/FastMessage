@echo off
chcp 65001 >nul
cd /d "%~dp0\examples\Demo"
call mvn clean compile -q
call mvn exec:java -Dexec.mainClass=fastmessaging.Demo -q
pause
