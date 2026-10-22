@echo off
chcp 65001 >nul
setlocal

REM ============================================================
REM  Dragon 龙族身份认证系统 —— 一键重启（先停后启）
REM  调用同目录的 stop-all.bat 与 start-all.bat
REM  ASCII 文件名版，可在命令行直接调用： cmd /c restart-all.bat
REM ============================================================

set ROOT=%~dp0

echo ============================================
echo   Dragon 龙族身份认证系统 - 一键重启
echo ============================================

call "%ROOT%stop-all.bat"
echo.
echo 等待端口释放（约 3 秒）...
timeout /t 3 >nul
echo.

call "%ROOT%start-all.bat"

endlocal
