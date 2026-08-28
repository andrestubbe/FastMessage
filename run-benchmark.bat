@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo [FastMessaging] Packaging core module...
call mvn clean package -DskipTests
cd examples\Benchmark
echo [FastMessaging] Building JMH Benchmark Suite...
call mvn clean package -DskipTests
echo [FastMessaging] Running JMH Benchmarks...
java -jar target\benchmarks.jar -f 1 -wi 3 -i 5
cd ..\..
pause
