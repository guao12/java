@echo off
chcp 936 >nul
cd /d "%~dp0"
title DragonRaja-初始化档案库
echo.
echo ============================================================
echo    卡塞尔学院 - 龙文档案馆  -  档案库初始化
echo ============================================================
echo.
echo    本脚本只建「档案库」的 5 张表并写入演示卷宗：
echo        dr_archive_category  档案分类（两级树）
echo        dr_archive           档案主表（含密级分级）
echo        dr_notice            首页公告
echo        dr_hot_keyword       热门检索词
echo        dr_search_log        检索日志
echo.
echo    安全说明：全部是 CREATE TABLE IF NOT EXISTS + INSERT IGNORE，
echo    不会 DROP 任何既有表，dr_user 里的账号数据完全不受影响，可重复执行。
echo.
REM ---- 自动探测 mysql.exe：兼容 MySQL 8.4 / 8.0 / 其他版本 ----
set "MYSQL_EXE="
if exist "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" set "MYSQL_EXE=C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
if not defined MYSQL_EXE if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" set "MYSQL_EXE=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
if not defined MYSQL_EXE for /d %%d in ("C:\Program Files\MySQL\*") do if exist "%%~fd\bin\mysql.exe" set "MYSQL_EXE=%%~fd\bin\mysql.exe"
if not defined MYSQL_EXE goto NO_MYSQL
set "SQL_FILE=..\springBootTest\sql\dragon_raja_archive.sql"
set "ERR_FILE=%TEMP%\dragon_archive_err.txt"
if not exist "%MYSQL_EXE%" goto NO_MYSQL
if not exist "%SQL_FILE%" goto NO_SQL
echo    [1/2] 正在建表并写入演示卷宗...
echo.
REM 导入用 utf8mb4（SQL 是 UTF-8 文件）；输出重定向到 nul 以避免 936 控制台乱码
"%MYSQL_EXE%" --default-character-set=utf8mb4 -uroot -p"123456" < "%SQL_FILE%" >nul
if errorlevel 1 goto FAILED
echo    [2/2] 校验结果:
echo.
REM 展示用 gbk：控制台是 936，查询结果用 utf8mb4 会乱码
"%MYSQL_EXE%" --default-character-set=gbk -uroot -p"123456" -e "USE dragon_raja; SELECT (SELECT COUNT(*) FROM dr_archive_category) AS 分类数, (SELECT COUNT(*) FROM dr_archive) AS 档案数, (SELECT COUNT(*) FROM dr_notice) AS 公告数, (SELECT COUNT(*) FROM dr_hot_keyword) AS 热词数;"
echo.
echo    [完成] 档案库已就绪。启动后端后首页与检索接口即可访问。
echo    提示：档案按密级分级展示，不同身份登录看到的内容不同。
echo.
pause
exit /b 0

:FAILED
"%MYSQL_EXE%" --default-character-set=utf8mb4 -uroot -p"123456" -e "source %SQL_FILE%" > "%ERR_FILE%" 2>&1
echo.
echo    [失败] 建表出错。请确认: 1) MySQL 服务已启动
echo           2) springBootTest\src\main\resources\application-dev.properties 里的账号密码正确
echo.
if exist "%ERR_FILE%" (
    echo    ---- MySQL 返回 ----
    type "%ERR_FILE%"
    del /q "%ERR_FILE%" >nul 2>&1
)
echo.
pause
exit /b 1

:NO_MYSQL
echo    [错误] 找不到 MySQL 客户端: %MYSQL_EXE%
echo           请把 mysql.exe 装到 C:\Program Files\MySQL\ 下，或手工改本脚本的 MYSQL_EXE
echo.
pause
exit /b 1

:NO_SQL
echo    [错误] 找不到 SQL 文件: %SQL_FILE%
echo           请确认脚本放在 DragonRaja-Vue 目录（与上级 springBootTest 同级）。
echo.
pause
exit /b 1
