@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

REM ============================================================
REM  Dragon 龙族身份认证系统 —— 一键启动（后端 + 前端）
REM  后端：Spring Boot  (dev, 端口 8082)
REM  前端：Vite         (端口 5177，/api 代理到 127.0.0.1:8082)
REM ============================================================

REM 关键：清掉沙箱/环境可能注入的 SERVER__PORT / SERVER_PORT，
REM 否则 Spring Boot 会把它解析成 server.port，覆盖 application-dev.properties 里的 8082
set SERVER__PORT=
set SERVER_PORT=

set ROOT=%~dp0
set BACKEND=%ROOT%springBootTest
set FRONTEND=%ROOT%DragonRaja-Vue
set PORT_BACK=8082
set PORT_FRONT=5177

echo ============================================
echo   Dragon 龙族身份认证系统 - 一键启动
echo ============================================

REM ---------------- 后端 ----------------
netstat -ano 2>nul | findstr ":%PORT_BACK% " | findstr "LISTENING" >nul
if !errorlevel!==0 (
    echo [后端] 端口 %PORT_BACK% 已被占用，跳过（如需重启请先运行 停止全部.bat）
) else (
    if not exist "%BACKEND%\target\springBootTest-0.0.1-SNAPSHOT.jar" (
        echo [后端] 未找到打包 jar，请先执行：cd springBootTest ^&^& mvnw clean package -DskipTests
    ) else (
        echo [后端] 正在启动 Spring Boot (dev, 端口 %PORT_BACK%)...
        start "Dragon-Backend" /D "%BACKEND%" cmd /c "java -Dserver.port=%PORT_BACK% -jar target\springBootTest-0.0.1-SNAPSHOT.jar > log\console.log 2>&1"
    )
)

REM ---------------- 前端 ----------------
netstat -ano 2>nul | findstr ":%PORT_FRONT% " | findstr "LISTENING" >nul
if !errorlevel!==0 (
    echo [前端] 端口 %PORT_FRONT% 已被占用，跳过
) else (
    echo [前端] 正在启动 Vite (端口 %PORT_FRONT%)...
    start "Dragon-Frontend" /D "%FRONTEND%" cmd /c "npm run dev > vite.log 2>&1"
)

echo.
echo 等待服务就绪（约 8 秒）...
timeout /t 8 >nul

echo.
echo ================= 启动结果 =================
echo  后端健康检查：
curl -s -m 5 http://127.0.0.1:%PORT_BACK%/api/health
echo.
echo  前端页面：    http://127.0.0.1:%PORT_FRONT%/
echo.
echo 演示账号：CC20260001 / CC20260002 / CC20260003    密钥：Dragon@2026
echo 需停止时运行：停止全部.bat
echo ============================================
endlocal
