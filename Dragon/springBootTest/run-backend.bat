@echo off
rem ============================================================
rem  springBootTest 后端启动脚本（供 Windows 计划任务托管使用）
rem  用法：直接双击，或由计划任务调用
rem  密钥来源：同目录 .run-secret（一行，不要有空格），避免写死在脚本里
rem ============================================================
setlocal
cd /d "%~dp0"

if exist "%~dp0.run-secret" (
  set /p DR_JWT_SECRET=<"%~dp0.run-secret"
)

if "%DR_JWT_SECRET%"=="" (
  echo [ERROR] 未找到 .run-secret 或内容为空，无法启动（dr.jwt.secret 必须注入）
  exit /b 1
)

set "JAVA_EXE=C:\Program Files\Zulu\zulu-21\bin\java.exe"
if not exist "%JAVA_EXE%" set "JAVA_EXE=java"

"%JAVA_EXE%" -jar "%~dp0target\springBootTest-0.0.1-SNAPSHOT.jar" --server.port=8082
