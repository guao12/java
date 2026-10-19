@echo off
chcp 936 >nul
cd /d "%~dp0"
title DragonRaja-重启项目
echo.
echo    正在停止旧进程...
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":5177" ^| findstr "LISTENING"') do (
    echo 正在停止前端  端口 5177  PID %%p
    taskkill /F /PID %%p >nul 2>&1
)
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":8082" ^| findstr "LISTENING"') do (
    echo 正在停止后端  端口 8082  PID %%p
    taskkill /F /PID %%p >nul 2>&1
)
ping -n 3 127.0.0.1 >nul
echo.
call "%~dp0启动.bat"
exit /b 0