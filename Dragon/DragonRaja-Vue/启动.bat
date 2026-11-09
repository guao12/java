@echo off
chcp 936 >nul
cd /d "%~dp0"
REM 注入 JWT 密钥：后端 dr.jwt.secret=${DR_JWT_SECRET} 没有默认值，缺了会直接启动失败
if exist "%~dp0..\springBootTest\.run-secret" (
    set /p DR_JWT_SECRET=<"%~dp0..\springBootTest\.run-secret"
    echo            已读取 springBootTest\.run-secret
) else (
    echo    [警告] 缺少 ..\springBootTest\.run-secret  后端将因无 DR_JWT_SECRET 启动失败
)

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
REM 后端启动策略：
REM   有 jar 且比源码新 -> 直接运行（重启最快）
REM   jar 缺失 / 比源码旧 -> 自动重新打包后再运行
REM 之前只看 jar 是否存在，改了 Java 代码照样跑旧 jar，
REM 容易出现「代码明明改了却不生效」，这里改为比对源码与 jar 的修改时间。
set "BACKEND=%~dp0..\springBootTest"
set "JAR=%BACKEND%\target\springBootTest-0.0.1-SNAPSHOT.jar"
set "NEED_BUILD=0"
set "SRC_NEWER="
if not exist "!JAR!" (
    set "NEED_BUILD=1"
    echo            未找到 jar  将首次构建后端
) else (
    REM 用 PowerShell 精确比较：src 与 pom.xml 中最新的修改时间是否晚于 jar
    REM 取不到结果（如 PowerShell 被禁用）时按「不用重建」处理，避免每次都白跑一次编译
    for /f "usebackq delims=" %%r in (`powershell -NoProfile -Command "$j=(Get-Item -LiteralPath '%JAR%').LastWriteTime; $s=Get-ChildItem -LiteralPath '%BACKEND%\src','%BACKEND%\pom.xml' -Recurse -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1; if($s -and $s.LastWriteTime -gt $j){'YES'}else{'NO'}" 2^>nul`) do set "SRC_NEWER=%%r"
    if /i "!SRC_NEWER!"=="YES" (
        set "NEED_BUILD=1"
        echo            检测到源码比 jar 新  需要重新打包
    )
)
if "!NEED_BUILD!"=="0" (
    echo            后端 jar 已是最新  直接运行 跳过编译
    start "龙族-后端-8082" cmd /k "cd /d "%BACKEND%" && set SERVER__PORT= && set SERVER__HOST= && java -jar "%JAR%" --server.port=8082"
) else (
    echo            正在构建后端  mvnw clean package 请稍候...
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
curl -s --noproxy "*" -o nul -m 2 "http://192.168.68.1:8082/api/health"
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
REM 取本机局域网 IP：优先「有默认网关且已连接」的网卡，避开 VMware / Hyper-V 虚拟网卡
set "LANIP="
for /f "usebackq delims=" %%i in (`powershell -NoProfile -Command "(Get-NetIPConfiguration | Where-Object { $_.IPv4DefaultGateway -ne $null -and $_.NetAdapter.Status -eq 'Up' } | Select-Object -First 1).IPv4Address.IPAddress" 2^>nul`) do set "LANIP=%%i"
if not defined LANIP (
    for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr /c:"IPv4"') do if not defined LANIP set "LANIP=%%a"
    if defined LANIP set "LANIP=!LANIP: =!"
)

echo ============================================================
echo    启动完成
echo.
echo      本机访问   http://192.168.68.1:5177
echo      后端接口   http://192.168.68.1:8082
if defined LANIP echo      局域网访问 http://!LANIP!:5177
echo      演示账号 CC20260001   密钥 Dragon@2026
echo.
echo    换电脑 / 多设备同步请注意：
echo      不要在另一台电脑上再点一次「启动.bat」——
echo      那样它会连自己那套 MySQL，权限进度在那边读不到，看起来就像被重置。
echo      正确做法：本机保持运行，另一台电脑浏览器打开上面的「局域网访问」地址，
echo                登录同一个账号，权限点与已完成任务会自动恢复。
echo.
echo    停止项目请双击  停止.bat
echo ============================================================
echo.
start "" "http://192.168.68.1:5177"
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