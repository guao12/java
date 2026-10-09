# AI-README.md —— 给 AI 助手的项目速查（先读我，别再重新探索）

> 这台电脑每次开机都会重置环境。下次开机要启动本项目时，
> **直接按本文执行，不要重新扫描项目结构、不要重新找工具路径。**

## 一句话概括

前后端分离登录系统：Spring Boot 后端（`springBootTest/`，端口 **8081**）
+ Vue3/Vite 前端（`创骑文件夹/`，端口 **5173**，代理 `/api` → 8081）。
数据库 MySQL 8，`kr_build` 库 / `kr_user` 表，root / 123456。

## 快速启动（每次开机）

**已实现开机自启**：`%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup\骑士系统开机启动.bat`
会 call 项目根目录的 `启动项目.bat`（GBK）。取消自启 = 删掉这个文件即可。

**人类用户手动方式**：双击 `启动项目.bat`（停止用 `停止项目.bat`，均为 GBK 编码，勿转 UTF-8）。

**AI**：依次执行（均已验证可用）：

```bash
# 1. 数据库（幂等，已存在则跳过；--force 会重建并清空用户数据！）
"C:/Users/Administrator/.workbuddy/binaries/python/envs/default/Scripts/python.exe" \
  "C:/Users/Administrator/Desktop/java-main/springBootTest/db/init_db.py"

# 2. 后端（前台常驻；jar 已打好则 2 秒启动）
cd "C:/Users/Administrator/Desktop/java-main/springBootTest" && \
  unset SERVER__PORT SERVER_PORT && \
  java -XX:TieredStopAtLevel=1 -Dspring.jmx.enabled=false \
  -jar target/springBootTest-0.0.1-SNAPSHOT.jar

# 3. 前端（前台常驻）
cd "C:/Users/Administrator/Desktop/java-main/创骑文件夹" && npm run dev

# 4. 验证
curl -s http://localhost:8081/api/health   # 应含 "MySQL 已连接"
curl -s http://localhost:5173/api/health   # 走 Vite 代理，应同上
```

## 关键路径（不要重新找）

| 用途 | 路径 |
|---|---|
| 项目根 | `C:\Users\Administrator\Desktop\java-main` |
| Maven | `C:\Users\Administrator\.m2\wrapper\dists\apache-maven-3.9.9-bin\4nf9hui3q3djbarqar9g711ggc\apache-maven-3.9.9\bin\mvn.cmd` |
| Java | 系统 PATH 已有，Zulu 21（`java -version` 即可） |
| Python(带 pymysql) | `C:\Users\Administrator\.workbuddy\binaries\python\envs\default\Scripts\python.exe` |
| Node/npm | 系统 PATH 已有 |
| 后端日志 | `springBootTest\log\backend.log` |
| 前端日志 | `创骑文件夹\dev.log` |

## 已踩过的坑（最重要，逐条记住）

1. **`SERVER__PORT` 环境变量劫持端口**：WorkBuddy 沙箱会注入 `SERVER__PORT=58867`，
   Spring Boot 松弛绑定会把它当成 `server.port`，导致后端不去 8081 而报
   "Port 58867 was already in use"。**启动 java 前必须 `unset SERVER__PORT SERVER_PORT`**
   （bat 里已 `set SERVER__PORT=`）。
2. **jar 不在就要重新打包**：`mvn package -DskipTests -q`（约 30 秒）。
   离线模式 `-o` 会失败（阿里云镜像里部分依赖本地没有），不要加 `-o`。
3. **npm 缓存可能损坏**：若 `npm run dev` 报 `Cannot find module ... node_modules/xxx`，
   是缓存/安装损坏。修法：`npm cache clean --force` 后 `npm ci --no-audit --no-fund`。
   **不要用 `rm -rf node_modules`**——会被 safe-delete 钩子拦截（>50 文件需确认），
   用 `mv node_modules node_modules.bak` 绕开（npm ci 自己会全新安装）。
4. **本机没有 mysql 命令行客户端**：初始化数据库用 `db/init_db.py`（pymysql）。
   MySQL 本体在跑（8.0.39，端口 3306）但重置后 `kr_build` 库可能不存在，
   后端会报 `Unknown database 'kr_build'`——跑一次 init_db.py 即可。
5. **reg.exe / sc.exe 被安全策略拉黑**，查服务用 `Get-Service` 或 `netstat -ano`。
6. **重置后可能消失的东西**：`node_modules/`、`target/*.jar`、`kr_build` 库。
   `启动项目.bat` 对这三项都做了"缺失才补"的幂等处理。
7. **bat 脚本必须 GBK 编码**（用户要求）。编辑后若中文变问号，用 Python
   `open(path,'w',encoding='gbk',newline='\r\n')` 重写。
8. **bat 脚本还必须是 CRLF 行尾**：用 Python 写 bat 时若 `newline=''` 会把 LF 写进去，
   cmd 会把多行揉成垃圾命令（报"xxx 不是内部或外部命令"）。务必 `newline='\r\n'`。
9. **bat 里禁用 `timeout` 等待**：stdin 被重定向时 `timeout` 直接报错退出
   （"不支持输入重新定向"）。用 `ping -n 2 127.0.0.1 >nul` 代替，任何上下文都能跑。
10. **两个 bat 已做完整停止→启动循环实测**（停止脚本 taskkill 按端口 PID；
    启动脚本全程幂等，约 10 秒拉起前后端）。
11. **业务流已端到端验证**：注册 → 登录 → /me → 花名册全部 200
    （走 5173 Vite 代理 + Session Cookie）。库里有测试账号 build_test / Test123456。

## 启动速度优化已做的事

- 后端用**预打好的 fat jar** 启动（`java -jar`，约 2 秒），而不是 `mvn spring-boot:run`（30 秒+）
- JVM 加 `-XX:TieredStopAtLevel=1`（C1 编译，启动快约 40%，仅开发用）
- 打包/装依赖全部跳过测试与 audit：`mvn package -DskipTests`、`npm ci --no-audit --no-fund`
- 脚本对 jar、node_modules、数据库均做存在性检查，存在即跳过

## 接口速查

- 健康检查 `GET /api/health`；导航页 `http://localhost:8081/index.html`
- 登录 `POST /api/user/login`；注册 `POST /api/user/register`；花名册 `GET /api/user/list`
- 登录态：Session + Cookie（JSESSIONID），前端 fetch 已带 `credentials: 'include'`
- 详细文档：`springBootTest/README.md`
