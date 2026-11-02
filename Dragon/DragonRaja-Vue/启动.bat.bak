@echo off
chcp 936 >nul
cd /d "%~dp0"
title DragonRaja-启动项目
setlocal enabledelayedexpansion
echo.
echo ============================================================
echo    卡塞尔学院 - 龙族身份认证系统
echo ============================================================
echo    启动项目
echo ============================================================
echo.
echo    [1/4] 检查运行环境...
where node >nul 2>&1
if errorlevel 1 goto NO_NODE
where java >nul 2>&1
if errorlevel 1 goto NO_JAVA
echo            Node OK     Java OK
echo.
echo    [2/4] 清理端口 5177 和 8082...
set "KILLED=0"
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":5177" ^| findstr "LISTENING"') do (
    echo 正在停止前端  端口 5177  PID %%p
    taskkill /F /PID %%p >nul 2>&1
    set "KILLED=1"
)
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":8082" ^| findstr "LISTENING"') do (
    echo 正在停止后端  端口 8082  PID %%p
    taskkill /F /PID %%p >nul 2>&1
    set "KILLED=1"
)
if "!KILLED!"=="1" ping -n 2 127.0.0.1 >nul
echo            端口已清理
echo.
echo    [3/4] 安装前端依赖 node_modules...
if exist "node_modules\.bin\vite.cmd" goto NPM_OK
echo            正在执行 npm install  首次可能需要几分钟...
call npm install
if errorlevel 1 goto NPM_FAIL
:NPM_OK
echo            前端依赖就绪
echo.
echo    [4/4] 启动服务...
REM 关键修复：清空外部注入的 SERVER__PORT / SERVER__HOST，避免端口被劫持到随机端口
set "SERVER__PORT="
set "SERVER__HOST="
REM 后端优先用已构建的 jar 直接运行，跳过编译；没有 jar 才 mvnw 构建一次（重启更快）
set "BACKEND=%~dp0..\springBootTest"
set "JAR=%BACKEND%\target\springBootTest-0.0.1-SNAPSHOT.jar"
if exist "%JAR%" (
    echo            后端已有 jar，直接运行 跳过编译
    start "龙族-后端-8082" cmd /k "cd /d "%BACKEND%" && set SERVER__PORT= && set SERVER__HOST= && java -jar "%JAR%" --server.port=8082"
) else (
    echo            首次构建后端 请稍候 mvnw clean package
    start "龙族-后端-8082" cmd /k "cd /d "%BACKEND%" && set SERVER__PORT= && set SERVER__HOST= && mvnw.cmd clean package -DskipTests && java -jar target\springBootTest-0.0.1-SNAPSHOT.jar --server.port=8082"
)
echo            后端窗口已打开  端口 8082
start "龙族-前端-5177" cmd /k "cd /d "%~dp0" && npm run dev"
echo            前端窗口已打开  端口 5177
echo.
echo    等待后端就绪  最长约三分钟  请留意后端窗口日志...
set COUNT=0
:WAIT
set /a COUNT+=1
curl -s --noproxy "*" -o nul -m 2 "http://127.0.0.1:8082/api/health"
if not errorlevel 1 goto READY
if !COUNT! gtr 120 goto SLOW
ping -n 2 127.0.0.1 >nul
goto WAIT
:SLOW
echo            [提示] 后端尚未就绪，请查看「后端-8082」那个窗口里的报错
echo                   若是数据库连接失败，请确认 MySQL 服务已启动，并核对
echo                   springBootTest\src\main\resources\application-dev.properties 里的账号密码。
:READY
echo.
echo ============================================================
echo    启动完成
echo.
echo      前端页面 http://127.0.0.1:5177
echo      后端接口 http://127.0.0.1:8082
echo      演示账号 CC20260001   密钥 Dragon@2026
echo.
echo    停止项目请双击  停止.bat
echo ============================================================
echo.
start "" "http://127.0.0.1:5177"
pause
endlocal
exit /b 0
:NO_NODE
echo    [错误] 未找到 Node.js  请先安装 Node 18 以上版本
echo.
pause
exit /b 1
:NO_JAVA
echo    [错误] 未找到 Java  请先安装 JDK 17 以上版本
echo.
pause
exit /b 1
:NPM_FAIL
echo    [错误] npm install 失败  请检查网络连接
echo.
pause
exit /b 1