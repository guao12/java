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
echo    [1/5] 检查运行环境...
where node >nul 2>&1
if errorlevel 1 goto NO_NODE
where java >nul 2>&1
if errorlevel 1 goto NO_JAVA
echo            Node OK     Java OK
echo.
echo    [2/5] 检查数据库 dragon_raja...
REM 自动探测 mysql.exe：兼容 MySQL 8.4 / 8.0 / 其他版本
set "MYSQL_EXE="
if exist "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" set "MYSQL_EXE=C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
if not defined MYSQL_EXE if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" set "MYSQL_EXE=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
if not defined MYSQL_EXE for /d %%d in ("C:\Program Files\MySQL\*") do if exist "%%~fd\bin\mysql.exe" set "MYSQL_EXE=%%~fd\bin\mysql.exe"
if not defined MYSQL_EXE goto NO_MYSQL
REM ===== 关键修复 C：不再用固定密码做命令行检查，也绝不自动初始化数据库 =====
REM 原因：MySQL 的 root 密码可能被改过。固定密码一旦失效，就会被误判成
REM       「数据库不存在」，进而自动执行 初始化数据库.bat ——
REM       而 sql\dragon_raja.sql 里有 DROP TABLE IF EXISTS dr_user，会清空已注册账号。
REM 数据库是否可用，改由后端启动后的 /api/health 判定（见 [5/5] 之后的等待逻辑）。
echo            已跳过命令行数据库检查
echo            数据库是否正常，稍后由后端健康检查确认
goto DB_DONE
:NEED_DB
REM 标签保留：仅作人工初始化提示，不再自动执行，避免误删数据
echo            [提示] 首次部署需要建库建表时，请手动双击  初始化数据库.bat
goto DB_DONE
:DB_DONE
echo    [3/5] 释放端口 5177 和 8082...
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":5177" ^| findstr "LISTENING"') do (
    echo 正在停止前端  端口 5177  PID %%p
    taskkill /F /PID %%p >nul 2>&1
)
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":8082" ^| findstr "LISTENING"') do (
    echo 正在停止后端  端口 8082  PID %%p
    taskkill /F /PID %%p >nul 2>&1
)
ping -n 3 127.0.0.1 >nul
echo            端口已清理
echo.
echo    [4/5] 安装前端依赖 node_modules...
if exist "node_modules\.bin\vite.cmd" goto NPM_OK
echo            正在执行 npm install  首次可能需要几分钟...
call npm install
if errorlevel 1 goto NPM_FAIL
:NPM_OK
echo            前端依赖就绪
echo.
echo    [5/5] 启动服务...
REM后端用 mvnw.cmd 本机未安装全局maven  在新窗口运行
REM ===== 关键修复 A：清空外部注入的 SERVER__PORT / SERVER__HOST =====
REM 某些环境（含部分 IDE / 容器 / 预览工具）会注入这两个环境变量，
REM Spring Boot 宽松绑定会把它们当成 server.port / server.address，
REM 于是后端不去用 dev 配置里的 8082，而去抢一个已被占用的随机端口，
REM 直接报 "Web server failed to start. Port xxx was already in use." 启动失败。
set "SERVER__PORT="
set "SERVER__HOST="
REM ===== 关键修复 B：显式传 --server.port=8082 兜底（命令行参数优先级最高）=====
start "龙族-后端-8082" cmd /k "cd /d "%~dp0..\springBootTest" && mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--server.port=8082"
echo            后端窗口已打开  端口 8082
start "龙族-前端-5177" cmd /k "cd /d "%~dp0" && npm run dev"
echo            前端窗口已打开  端口 5177
echo.
echo    等待服务就绪  最长约两分钟  请留意后端窗口日志...
set COUNT=0
:WAIT
set /a COUNT+=1
curl -s -o nul -m 2 "http://127.0.0.1:8082/api/health"
if not errorlevel 1 goto READY
if !COUNT! gtr 30 goto SLOW
ping -n 5 127.0.0.1 >nul
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
:NO_MYSQL
echo    [警告] 未找到 mysql.exe  跳过数据库检查
echo            若登录报错 请手动运行 初始化数据库.bat
echo.
goto DB_DONE

:NPM_FAIL
echo    [错误] npm install 失败  请检查网络连接
echo.
pause
exit /b 1