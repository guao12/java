@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

REM ============================================================
REM  Dragon 龙族身份认证系统 —— 一键停止（后端 + 前端）
REM  按监听端口 8082 / 5177 精准结束进程，不影响其他 java/node
REM ============================================================

set PORT_BACK=8082
set PORT_FRONT=5177

echo ============================================
echo   Dragon 龙族身份认证系统 - 一键停止
echo ============================================

REM ---------------- 后端：端口 8082 ----------------
set KILLED=0
for /f "tokens=5" %%a in ('netstat -ano 2^>nul ^| findstr ":%PORT_BACK% " ^| findstr "LISTENING"') do (
    echo [后端] 结束 PID %%a (端口 %PORT_BACK%)
    taskkill /PID %%a /F >nul 2>&1
    set KILLED=1
)
if !KILLED!==0 echo [后端] 端口 %PORT_BACK% 无监听进程

REM ---------------- 前端：端口 5177 ----------------
set KILLED=0
for /f "tokens=5" %%a in ('netstat -ano 2^>nul ^| findstr ":%PORT_FRONT% " ^| findstr "LISTENING"') do (
    echo [前端] 结束 PID %%a (端口 %PORT_FRONT%)
    taskkill /PID %%a /F >nul 2>&1
    set KILLED=1
)
if !KILLED!==0 echo [前端] 端口 %PORT_FRONT% 无监听进程

echo.
echo 停止完成。可用下方命令确认端口已释放：
echo   netstat -ano ^| findstr ":%PORT_BACK% " ^| findstr "LISTENING"
echo   netstat -ano ^| findstr ":%PORT_FRONT% " ^| findstr "LISTENING"
echo ============================================
endlocal
