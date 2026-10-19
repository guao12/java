@echo off
chcp 936 >nul
cd /d "%~dp0"
title DragonRaja-初始化数据库
echo.
echo ============================================================
echo    卡塞尔学院 - 龙族身份认证系统  -  数据库初始化
echo ============================================================
echo.
REM 自动探测 mysql.exe：兼容 MySQL 8.4 / 8.0 / 其他版本
set "MYSQL_EXE="
if exist "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" set "MYSQL_EXE=C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
if not defined MYSQL_EXE if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" set "MYSQL_EXE=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
if not defined MYSQL_EXE for /d %%d in ("C:\Program Files\MySQL\*") do if exist "%%~fd\bin\mysql.exe" set "MYSQL_EXE=%%~fd\bin\mysql.exe"
if not defined MYSQL_EXE goto NO_MYSQL
set "SQL_FILE=..\springBootTest\sql\dragon_raja.sql"
set "ERR_FILE=%TEMP%\dragon_raja_err.txt"
echo    数据库 : dragon_raja    账号: root    密码: 见 application-dev.properties
echo.
if not exist "%MYSQL_EXE%" goto NO_MYSQL
if not exist "%SQL_FILE%" goto NO_SQL

echo    [1/2] 正在创建库表并写入演示账号...
echo.
REM 导入用 utf8mb4：SQL 是 UTF-8 文件，用默认 GBK 解析会报 ERROR 1366 且数据写不进去。
REM 输出重定向到 nul：SQL 末尾的中文回显在 936 控制台会显示乱码。
"%MYSQL_EXE%" --default-character-set=utf8mb4 -uroot -p"123456" < "%SQL_FILE%" >nul
if errorlevel 1 goto FAILED

echo    [2/2] 校验结果:
echo.
REM 展示用 gbk：控制台是 936，查询结果若用 utf8mb4 输出会显示乱码。
"%MYSQL_EXE%" --default-character-set=gbk -uroot -p"123456" -e "USE dragon_raja; SELECT account, codename, identity FROM dr_user;"
echo.
echo    [完成] dragon_raja 数据库已就绪。
echo    演示密钥统一为  Dragon@2026
echo.
pause
exit /b 0

:FAILED
"%MYSQL_EXE%" --default-character-set=utf8mb4 -uroot -p"123456" -e "source %SQL_FILE%" > "%ERR_FILE%" 2>&1
echo.
echo    [失败] 初始化出错。请确认: 1) MySQL 服务已启动  2) root 密码与 application-dev.properties 一致
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
echo    [错误] 找不到 MySQL 客户端:
echo            %MYSQL_EXE%
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
