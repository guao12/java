@echo off
chcp 936 >nul
cd /d "%~dp0"
title DragonRaja-停止项目
echo.
echo ============================================================
echo    卡塞尔学院 - 龙族身份认证系统
echo ============================================================
echo    停止项目
echo ============================================================
echo.
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
echo    [完成] 项目已停止。
echo.
pause
exit /b 0