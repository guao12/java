@echo off
chcp 936 >nul
setlocal enabledelayedexpansion
cd /d "%~dp0"
title 龙族-后端-开发模式
REM ============================================================
REM  开发模式启动：走 Maven spring-boot:run，devtools 生效
REM  （改完代码执行一次编译，进程会自动重启，不用重新 package 起 jar）
REM
REM  为什么不用 java -jar：spring-boot-maven-plugin 的 repackage 默认
REM  excludeDevtools=true，devtools 根本不在 fat jar 里，热重启不会触发。
REM
REM  用法：dev-run.bat            正常启动（端口 8082）
REM        dev-run.bat --check    只打印探测结果，不启动
REM ============================================================

REM 外部环境可能注入 SERVER__PORT，会把端口劫持到随机值，这里清掉
set "SERVER__PORT="
set "SERVER__HOST="

REM ---------- 1. 读取 JWT 密钥 ----------
set "JWT_STATE=未设置"
if exist "%~dp0.run-secret" (
    set /p DR_JWT_SECRET=<"%~dp0.run-secret"
    set "JWT_STATE=已读取"
) else (
    echo    [警告] 缺少 .run-secret  后端会因无 DR_JWT_SECRET 启动失败
)

REM ---------- 2. 探测 Maven 发行版 ----------
set "MVN_HOME="
set "BOOT_JAR="
set "DISTS=%USERPROFILE%\.m2\wrapper\dists"

if exist "%DISTS%" (
    for /f "delims=" %%V in ('dir /b /ad /o-n "%DISTS%\apache-maven-*-bin" 2^>nul') do (
        if not defined MVN_HOME (
            for /d %%H in ("%DISTS%\%%V\*") do (
                if not defined MVN_HOME (
                    for /d %%M in ("%%H\apache-maven-*") do (
                        if exist "%%M\boot" set "MVN_HOME=%%M"
                    )
                )
            )
        )
    )
)

if not defined MVN_HOME (
    where mvn >nul 2>&1
    if errorlevel 1 (
        echo    [错误] 找不到 Maven：PATH 里没有 mvn，%DISTS% 下也没有可用发行版
        echo           先执行一次 mvnw.cmd -v 让 wrapper 把发行版下载下来
        exit /b 1
    )
)

REM ---------- 3. 定位 plexus-classworlds jar ----------
if defined MVN_HOME (
    for %%J in ("%MVN_HOME%\boot\plexus-classworlds-*.jar") do set "BOOT_JAR=%%J"
    if not defined BOOT_JAR (
        echo    [错误] %MVN_HOME%\boot 下没有 plexus-classworlds jar
        exit /b 1
    )
)

set "JH=%JAVA_HOME%"
if "!JH:~-1!"=="\" set "JH=!JH:~0,-1!"
set "JAVA_EXE=!JH!\bin\java.exe"
if not exist "!JAVA_EXE!" set "JAVA_EXE=java"

REM ---------- 4. 自检模式 ----------
if /i "%~1"=="--check" (
    echo MVN_HOME=!MVN_HOME!
    echo BOOT_JAR=!BOOT_JAR!
    echo JAVA_EXE=!JAVA_EXE!
    echo JWT_STATE=!JWT_STATE!
    if defined DR_JWT_SECRET (
        echo JWT_HEAD=!DR_JWT_SECRET:~0,6!
    ) else (
        echo JWT_HEAD=EMPTY
    )
    exit /b 0
)

REM ---------- 5. 启动 ----------
echo    Maven   : !MVN_HOME!
echo    密钥    : !JWT_STATE!
echo    端口    : 8082
echo    热重启  : devtools 已启用，改代码后编译一次即自动重启
echo.

if defined MVN_HOME (
    "%JAVA_EXE%" -classpath "!BOOT_JAR!" ^
        "-Dclassworlds.conf=!MVN_HOME!\bin\m2.conf" ^
        "-Dmaven.home=!MVN_HOME!" ^
        "-Dmaven.multiModuleProjectDirectory=%~dp0." ^
        org.codehaus.plexus.classworlds.launcher.Launcher ^
        spring-boot:run "-Dspring-boot.run.arguments=--server.port=8082"
) else (
    call mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=8082"
)
