@echo off
chcp 936 >nul
cd /d "%~dp0"
title 龙族社区 - 一键启动
echo.
echo ============================================================
echo    龙族读者社区 - 统一启动入口
echo    后端 springBootTest  : 8082
echo    前端 DragonRaja-Vue  : 5177
echo ============================================================
echo.

REM ---------- 1. 数据库前置检查 ----------
REM 3306 没监听时后端会在 Hikari 建连阶段直接失败，先提示比让用户看堆栈友好
netstat -ano | findstr ":3306" | findstr "LISTENING" >nul 2>&1
if errorlevel 1 (
    echo    [警告] 本机 3306 端口没有监听，MySQL 可能未启动。
    echo           后端需要 dragon_raja 库，请先启动 MySQL 服务。
    echo.
    choice /C YN /M "仍要继续启动吗"
    if errorlevel 2 exit /b 1
) else (
    echo    MySQL 3306 已就绪
)
echo.

REM ---------- 2. 交给前端目录的完整启动器 ----------
REM DragonRaja-Vue\启动.bat 已包含：依赖安装、端口清理、后端按需打包、前后端拉起
if not exist "%~dp0DragonRaja-Vue\启动.bat" (
    echo    [错误] 找不到 DragonRaja-Vue\启动.bat，目录结构可能被改动过。
    pause
    exit /b 1
)

call "%~dp0DragonRaja-Vue\启动.bat"
