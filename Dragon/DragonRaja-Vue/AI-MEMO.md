# AI 备忘：DragonRaja-Vue 项目上下文

> **给 AI 助手（以及未来的你）看的。**
> 本机每次重置，`node_modules/` 和 `target/` 会被删除。
> 下次接手本项目时：**先读这份文件，不要重新探索整个代码库。**
> 读完直接执行文末的「快速启动」，通常 1 分钟内可跑起来。

> **⚠️ 2026-10-15 结构变更：项目已整体搬迁到 `C:\Users\Administrator\Desktop\Dragon\`**
> 前端 = `Dragon\DragonRaja-Vue\`，后端 = `Dragon\springBootTest\`（**兄弟目录**，不再是前端子目录）。
> 脚本里的后端路径一律写 `..\springBootTest`。

---

## 0. 这是什么项目

龙族（Dragon Raja）同人风格的**登录 / 注册 / 密钥重置 + 首页档案检索**系统。
两个可独立运行的部分，共用一份代码仓库：

| 部分 | 目录 | 技术栈 | 端口 |
| --- | --- | --- | --- |
| 前端 | `Dragon\DragonRaja-Vue\` | Vue 3.5 + Vue Router 4 + Vite 6 | **5177** |
| 后端 | `Dragon\springBootTest\` | Spring Boot 3.4.5 + MyBatis-Plus 3.5.9 + MySQL 8 | **8082** |

数据流：`浏览器 → Vite(5177) --代理 /api--> Spring Boot(8082) --MyBatis-Plus--> MySQL(dragon_raja)`

前端是纯静态 SPA，用 **Hash 路由**（`createWebHashHistory`），所以 `dist/` 可直接双击打开或丢给任意静态服务器。

---

## 1. 启动前必读：七个致命坑（别再踩）

### 坑 1：数据库 `dragon_raja` 默认不存在

MySQL 8.0 已安装并常驻运行，但**默认只有 `ruoyi` / `kr_build` 等库，没有 `dragon_raja`**。
后端启动后如果 `dragon_raja` 不存在，登录接口会返回 `{"code":500,"message":"服务器开小差了，请稍后再试"}`。

**判断方法**（一条命令）：
```bash
"/c/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe" -uroot -p123456 -e "SHOW DATABASES LIKE 'dragon_raja';"
```
有输出 `dragon_raja` = 正常；空 = 需要初始化，执行 `初始化数据库.bat`。

> ⚠️ **陷阱**：`/api/health` 返回 `"MySQL 已连接"` **不代表数据库正确**。
> 旧版本后端的 health 里写死了这行文案，与真实连的是哪个库无关。
> **判断后端是否真的可用，唯一可靠方式是调登录接口，或直接查库。**

### 坑 2：SQL 脚本必须显式指定 utf8mb4，否则中文乱码失败

`sql/dragon_raja.sql` 是 **UTF-8** 编码，但 MySQL 客户端默认按 GBK 解析，
直接执行会报 `ERROR 1366 (HY000): Incorrect string value ... for column 'codename'`，
结果是**库表建好了、演示数据一条没进去**（最阴险：看起来成功了，实际是空的）。

**导入用 utf8mb4，展示查询用 gbk**：
```
REM 导入（读 UTF-8 文件，必须 utf8mb4）
"%MYSQL_EXE%" --default-character-set=utf8mb4 -uroot -p123456 < "sql\dragon_raja.sql" >nul

REM 展示（控制台是 936，用 gbk 才不乱码）
"%MYSQL_EXE%" --default-character-set=gbk -uroot -p123456 -e "SELECT ...;"
```

### 坑 3（2026-10-15 新踩）：`SERVER__PORT` 环境变量劫持端口，后端启动即失败

**症状**：`mvnw.cmd spring-boot:run` 起来后报

```
Tomcat initialized with port 64581 (http)        ← 不是 8081！是个随机端口
APPLICATION FAILED TO START
Description: Web server failed to start. Port 64581 was already in use.
```

**原因**：某些运行环境（**实测：WorkBuddy / CodeBuddy 等带预览服务的环境**）会往进程里注入两个环境变量：

```
SERVER__PORT=64581      ← 注意是「双下划线」
SERVER__HOST=127.0.0.1
```

Spring Boot 的**宽松绑定**会把它们当成 `server.port` / `server.address`。
而 **环境变量的优先级高于 `application-dev.properties`**，所以配置里写的 `server.port=8082` 被直接盖掉，
后端转而去绑定那个已被预览服务占用的随机端口 → 必然失败。

**排查方法**（一条命令，比猜快一万倍）：

```bash
env | grep -i "^SERVER"        # 会看到 SERVER__PORT / SERVER__HOST
# 或在 maven 调试日志里搜：mvnw.cmd -X spring-boot:run  → 搜 env.SERVER__PORT
```

> ⚠️ 用 `grep -i server_port` 是**搜不到**的，因为是双下划线 `SERVER__PORT`。
> 同理 `/api/health` 里的 `port` 字段、`application*.properties` 都查不出这个问题。

**修复**（`启动.bat` 已内置，双重保险）：

```bat
REM 1. 清空外部注入的环境变量（子进程继承，所以后端窗口里就干净了）
set "SERVER__PORT="
set "SERVER__HOST="
REM 2. 显式指定端口，命令行参数优先级最高，谁都盖不掉
start "龙族-后端-8082" cmd /k "cd /d "%~dp0..\springBootTest" && mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--server.port=8082"
```

优先级顺序记牢：**命令行参数 > 系统属性(-D) > 环境变量 > application-{profile}.properties**。
所以碰到「配置明明写对了却生效不了」，一律用 `--xxx=yyy` 命令行参数兜底。

### 坑 4（2026-10-15 新踩）：MySQL 版本升级到 8.4，脚本路径写死 8.0 会导致静默跳过

AI-MEMO 和脚本原先都假设 mysql.exe 在 `C:\Program Files\MySQL\MySQL Server 8.0\`。
**本机（2026-10-15 实测）装的是 `MySQL Server 8.4`**，路径不同：

```
C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe
```

后果：老版 `启动.bat` 里 `if not exist "%MYSQL_EXE%" goto NO_MYSQL` 会命中 `NO_MYSQL` 分支，
而 `NO_MYSQL` **没有 exit，直接落到 `:NPM_FAIL` 标签**，于是打印「npm install 失败」并 `exit /b 1`
—— 表现就是「双击启动.bat 一闪而过，什么都没起来」。

**修复**：改为自动探测，8.4 / 8.0 / 其他版本通吃（`启动.bat`、`初始化数据库.bat` 均已内置）：

```bat
set "MYSQL_EXE="
if exist "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" set "MYSQL_EXE=C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
if not defined MYSQL_EXE if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" set "MYSQL_EXE=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
if not defined MYSQL_EXE for /d %%d in ("C:\Program Files\MySQL\*") do if exist "%%~fd\bin\mysql.exe" set "MYSQL_EXE=%%~fd\bin\mysql.exe"
if not defined MYSQL_EXE goto NO_MYSQL
```

同时把 `:NO_MYSQL` 分支改成 `goto DB_DONE`（只警告、不再误退出整个脚本）。
`初始化数据库.bat` 的 `:FAILED` 分支也不要再写死路径，统一用 `%MYSQL_EXE%`。

---

### 坑 7（2026-10-15 新踩）：MyBatis 注解 SQL 里 `&lt;=` 只在 `<script>` 块内生效

用 `@Select` 写动态 SQL 时，必须包一层 `<script>`，MyBatis 才会按 XML 解析，
此时 `<`、`<=`、`>` 必须写成 `&lt;`、`&lt;=`、`&gt;`。

**但普通（没有 `<script>` 的）`@Select` 不会做任何转义**，写 `&lt;=` 会原样拼进 SQL，
直接报 `SQLSyntaxErrorException: ... near '= 1 GROUP BY ...'` —— 报错信息里看不到 `<=`，
很容易误判成 SQL 写错。

```java
// ✅ 动态 SQL：包 <script>，用 XML 转义
@Select("<script>SELECT * FROM t WHERE a.security_level &lt;= #{maxLevel} "
      + "<if test='kw != null'>AND ...</if></script>")

// ✅ 静态 SQL：不包 <script>，直接写 <=
@Select("SELECT * FROM t WHERE security_level <= #{maxLevel}")
```

排查技巧：看日志里 `Preparing:` 那一行，转义没被还原说明块外写了 `&lt;`。

---

## 1.5 写 .bat 脚本必须避开的 3 个 cmd 坑（本项目已实测踩过）

这几个坑非常隐蔽，**症状是脚本中途报 `此时不应有 ...` 然后静默退出（退出码 255）**。

| 坑 | 现象 | 正确做法 |
| --- | --- | --- |
| **GBK 文件里`echo` 行内出现半角括号 `()`** | 报`此时不应有 ...`，脚本中断 | echo 提示语里**不要用半角括号**，改用全角`【】`、空格或引号包裹 |
| **`if` 代码块内做输入重定向 `<`** | 报 `此时不应有 MySQL` | 用 `goto :LABEL` 标签替代 `if` 块，不要在块内做重定向 |
| **`timeout /t N`** | Git Bash 下报 `invalid time interval` | 改用 `ping -n 3 127.0.0.1 >nul` 延时，Windows 与 Git Bash 通用 |

> 排查这类问题的方法：把脚本逐段塞进 `.bat` 真实执行，
> **用 Python `subprocess` 跑并按 GBK 解码 stdout/stderr**，不要凭猜测改。
> 参考：本次问题就是这样定位到「GBK + 半角括号」的。

**编码配套写法**：用 Python 生成 bat 时必须**二进制写 + GBK 编码 + CRLF**：
```python
with open(path, "wb") as f:
    f.write("\r\n".join(lines).encode("gbk"))
```
用 `open(..., "w")` 会把 CRLF 变成 LF，**批处理会解析失败**。

---

### 坑 5（2026-10-15 新踩）：`http_proxy` 让 curl 读到「缓存的旧响应」，排查会被严重误导

- 本环境注入了 `http_proxy` / `HTTP_PROXY`（指向 `127.0.0.1:60812` 之类的本地代理）。
- **`curl http://127.0.0.1:8082/...` 会走这个代理** → 代理缓存旧结果，于是出现
  「netstat 里端口明明没进程，curl 却能返回一份 `userCount:0` 的 health JSON」这种鬼现象。
- 正确写法：**所有本机端口验证都加 `--noproxy '*'`**
  `curl -s --noproxy '*' http://127.0.0.1:8082/api/health`
- Vite 的 `/api` 代理是 node 直连，**不受影响**（日志里 `connect ECONNREFUSED` 可证），
  所以这个坑只影响「排查时读到的信息」，不影响应用本身。
- 结论：判断端口占用请以 **`netstat` + 进程命令行** 为准，不要只信 curl 的返回。

### 坑 6（2026-10-15 新踩）：桌面上另一份副本 `Desktop\java-main` 会「杀掉」Dragon 的后端

- `java-main` 是同一项目的另一份副本：前端 `frontend`（5173）+ 后端 `springBootTest`（跑 `target/*.jar`）。
- 它的 `start-all.bat` / `stop-all.bat` 里有 `:killport 8081` → `taskkill /PID %%p /F`；
  Dragon 自己的 `启动.bat` 也会 killport 自己的端口 —— **两套脚本同时跑就会互相杀**。
- 现象：从 AI 会话里起的服务过几分钟就 `Process terminated with exit code: 1`，
  日志里看不到任何异常，只是「进程没了」，极易误判成"程序有 bug"。
- 一眼分辨端口上跑的是哪一份：
  Dragon 后端 = 命令行含 `mvnw spring-boot:run`，health 返回 `MySQL 已连接（dragon_raja）` + `userCount`；
  java-main 后端 = 命令行含 `java -jar target\springBootTest-0.0.1-SNAPSHOT.jar`，health 只有 `MySQL 已连接`。
- **最终解法（2026-10-15 已实施）：Dragon 后端端口 8081 → 8082**，与 java-main 彻底错开，
  两边可同时运行。以后再改端口，以下位置要一起改：
  - `springBootTest\src\main\resources\application-dev.properties` 的 `server.port`
  - `DragonRaja-Vue\vite.config.js` 的代理 target、`src\api\http.js`、`src\views\LoginView.vue` 文案
  - `启动.bat` / `停止.bat` / `重启.bat`（killport、窗口标题、`--server.port=`、curl 健康检查地址、提示文案）
  - `README.md` / `AI-MEMO.md` / 后端 `static\index.html` 的端口描述
  - ⚠️ `application-prod.properties` 本来就是 8082，**dev 与 prod 现在端口相同**，本地跑 prod 需先改
- 经验：长期运行请让用户**双击 `启动.bat` 用自己的窗口**，别依赖 AI 会话里起的进程（会被脚本杀、也会随会话回收）。

## 2. 环境现状（已实测，非推测）

| 项目 | 状态 | 说明 |
| --- | --- | --- |
| Node.js | ✅ v22.22.2 / npm 10.9.7 | 满足 Vite 6 要求 |
| Java | ✅ Oracle JDK 17.0.12 | Spring Boot 3 要求 17+；21 也跑过，都行 |
| Maven（全局） | ❌ **未安装** | 不要用 `mvn` 命令 |
| Maven Wrapper | ✅可用 | 用 `springBootTest\mvnw.cmd`，见下方说明 |
| MySQL 8.4 | ✅已装且运行中 | `root` / `123456`，服务常驻 3306（**路径是 8.4 不是 8.0**，见 §1 坑 4） |
| `mysql.exe` | ⚠️ 不在 PATH | 必须用完整路径，见 §1；脚本已自动探测版本 |
| `SERVER__PORT` 等注入变量 | ⚠️ 视运行环境而定 | 会劫持后端端口，见 §1 坑 3 |

### 关于 Maven：没有全局 mvn，怎么办

`mvnw.cmd` 可直接用，无需安装 Maven。它是 **Maven Wrapper 3.3.2「仅脚本」模式**：
`.mvn/wrapper/` 下**只有 `maven-wrapper.properties`、没有 `maven-wrapper.jar`，这是正常的**。
首次运行会自动从阿里云镜像下载 Maven 3.9.9 到 `~/.m2/wrapper/dists/`。
本机 `~/.m2/repository` **已有大量缓存依赖**（177MB+，含 Spring Boot 全家桶），所以离线或快速构建基本没问题。

**如果哪天真装了全局 Maven**，`mvn` 也能用；但为了与脚本一致，**始终优先用 `mvnw.cmd`**。

---

## 3. 关键配置速查（不要凭记忆改这些值）

**前端 `vite.config.js`**
- `base: './'`（相对路径，dist 可随处部署）
- `server.port: 5177`，`server.host: 127.0.0.1`
- 代理：`/api` → `http://127.0.0.1:8082`（可用环境变量 `VITE_API_TARGET` 覆盖）
- 别名：`@` → `./src`

**前端 API 基址 `src/api/http.js`**
- 开发环境**留空**（走 Vite 代理，无跨域问题）
- 仅部署时用 `.env.production` 的 `VITE_API_BASE` 指向后端
- 统一响应体：`{code, message, data}`，**`code === 0` 才算成功**，401 自动清会话跳回登录页

**后端 `springBootTest/src/main/resources/`**
- `application.properties`：`spring.profiles.active=dev`，JWT 短令牌 12h / 长令牌（记住身份卡）7 天
- `application-dev.properties`：**端口 8082**，数据源 `jdbc:mysql://localhost:3306/dragon_raja`，`root/123456`
- 另有 `application-test.properties`(8080) / `application-prod.properties`(8082)
- JWT 密钥可用环境变量 `DR_JWT_SECRET` 覆盖（默认已内置一个 64 位值，够开发用）

> 注意：`target/classes/` 下有一份 **旧拷贝** 的 properties，重置后 `target` 会被删，
> 真实配置源永远是 `springBootTest/src/main/resources/`，别改错地方。

---

## 4. 目录结构（只列关键节点）

```
Dragon/
├─ springBootTest/                ← 后端（与前端平级）
│  ├─ mvnw / mvnw.cmd             ← Maven Wrapper（仅脚本模式）
│  ├─ pom.xml                    ← java 17, Spring Boot 3.4.5, MyBatis-Plus 3.5.9, jjwt 0.12.6
│  ├─ sql/dragon_raja.sql        ← 建库建表 + 3 个演示账号（UTF-8，务必配 utf8mb4 执行）
│  ├─ src/main/java/com/fenglin/springboottest/
│  │  ├─ controller/             ← AuthController HealthController ProfileController
│  │  ├─ service/ impl/          ← AuthServiceImpl DictServiceImpl JwtTokenService
│  │  ├─ mapper/ entity/ dto/ vo/
│  │  └─ config/                 ← JwtInterceptor WebMvcConfig MybatisPlusConfig AppConfig
│  ├─ src/main/resources/application*.properties
│  └─ target/                    ← 每次重置后被删，需重新构建
└─ DragonRaja-Vue/                ← 前端
   ├─ 启动.bat / 停止.bat / 重启.bat / 初始化数据库.bat   ← GBK 编码，后端路径写 ..\springBootTest
   ├─ AI-MEMO.md / AI-MEMO.txt
   ├─ README.md                  ← 官方说明（里面后端路径写的是 D:\workSpace\springBootTest，已过时）
   ├─ index.html                 ← 入口（含内联 SVG favicon）
   ├─ vite.config.js  package.json  package-lock.json
   ├─ dist/                      ← 构建产物，base:'./' 可直接打开
   ├─ src/
   │  ├─ main.js  App.vue        ← App.vue 按 meta.standalone 决定是否套认证页外壳
   │  ├─ router/index.js         ← /home /login /register /forgot /console（Hash 模式；/home /console 需鉴权）
   │  ├─ styles/theme.css        ← 设计令牌，换肤改这里 :root 的 --gold/--blood/--ice
   │  ├─ api/http.js  api/auth.js  api/search.js  ← search 为 /api/search 预留位 + 本地演示数据回退
   │  ├─ utils/auth.js           ← token 存取、清理
   │  ├─ components/             ← AuthLayout RuneBackground DragonEmblem AuthCard FormField GlowButton AppIcon
   │  └─ views/                  ← HomeView LoginView RegisterView ForgotView ConsoleView
```

---

## 5. 后端接口清单

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | 新生登记，返回签发学号 | 否 |
| POST | `/api/auth/login` | 认证，返回 JWT + 用户信息 | 否 |
| GET | `/api/auth/check-account` | 账号是否已登记（返回行动代号） | 否 |
| POST | `/api/auth/reset/send` | 下发 6 位验证龙文（5min 有效 / 60s 冷却） | 否 |
| POST | `/api/auth/reset/confirm` | 校验龙文并重置密钥 | 否 |
| GET | `/api/auth/dictionaries` | 身份 / 言灵字典 | 否 |
| GET | `/api/user/profile` | 我的身份卡 | 是 |
| POST | `/api/user/logout` | 退出登录 | 是 |
| GET | `/api/health` | 健康检查（**文案有误导性，见 §1**） | 否 |
| GET | `/api/search` | **预留位：首页档案检索**。后端未实现，前端回退本地演示数据 | 否 |

> **首页说明（2026-10-15 新增）**：登录成功后跳转 `/home`（HomeView.vue）。
> 含导航栏（品牌 + 菜单 + 用户下拉/退出）和搜索栏；风格与登录页同源（theme.css 设计令牌）。
> `/api/search?keyword=xxx` 是后端预留接口，实现后前端自动切换真实数据（api/search.js）。
> **登录已改为纯学号体系（邮箱已全链路移除）**：演示账号 `CC20260001` / 密钥 `Dragon@2026`。

---

## 5.3 档案库：首页展示 + 分级分类检索（2026-10-15 完成）

### 建表

`sql/dragon_raja_archive.sql` —— 5 张表，**全部 `CREATE TABLE IF NOT EXISTS` + `INSERT IGNORE`，
不含任何 `DROP`，可重复执行，且完全不动 `dr_user`**：

| 表 | 用途 |
| --- | --- |
| `dr_archive_category` | 档案分类，**两级树**（`parent_id=0` 为一级）。ID 固定分配：一级 1001+，二级 2001+ |
| `dr_archive` | 档案主表，核心字段 `security_level`（密级 1-4） |
| `dr_notice` | 首页公告（`level` 1普通/2重要/3紧急，`pinned` 置顶，`expire_at` 空=长期） |
| `dr_hot_keyword` | 热门检索词 |
| `dr_search_log` | 检索日志（统计热词 + 审计） |

种子数据：21 个分类（6 一级 + 15 二级）、35 条卷宗、3 条公告、8 个热词。

双击 `DragonRaja-Vue\初始化档案库.bat` 即可建库。

### 「分级」与「分类」分别指什么

- **分级 = 密级**：公开(1) / 内部(2) / 机密(3) / 绝密(4)。
  可见上限由在校身份决定，规则在 `common/SecurityLevel`：
  `guest`/未登录 → 1，`freshman`/`student` → 2，`executor` → 3，`teacher` → 4。
  所有查询都带 `security_level <= maxLevel`；越级筛选自动收敛；按 id 越级访问返回 400。
- **分类 = 两级分类树**：一级（人物志 / 言灵谱 / 地理志 / 机构志 / 文献卷 / 制度典）
  + 二级（混血种名册 / 龙王系谱 / 攻击型言灵…）。

### 接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/home/overview` | 首页概览：通报 + 统计 + 两级分类 + 精选/热门/最新 + 热词 |
| GET | `/api/search` | 检索：`keyword` `topCategory` `category` `level` `sort` `page` `size` |
| GET | `/api/search/categories` | 两级分类树（带可见卷宗数） |
| GET | `/api/search/hot` | 热词 |
| GET | `/api/archive/{id}` | 详情（浏览量 +1） |

### 两个设计要点

1. **`OptionalAuthInterceptor`（软鉴权）** —— 首页/检索对所有人开放，但带令牌就解析身份。
   注册在 `WebMvcConfig`，与强制鉴权的 `JwtInterceptor`（仅 `/api/user/**`）互不重叠。
   记得 `afterCompletion` 里 `CurrentUser.clear()`，否则线程复用会串号。
2. **分面统计（筛选项旁的条数）口径一致** ——
   `facetTopCategory` / `facetCategory` / `facetLevel` 三个维度
   **都只对「其他维度」的条件生效、忽略自身维度**，
   所以「一级合计 == 二级合计 == 密级合计 == 命中总数」恒成立。
   否则二级分类的条数会和一级对不上，看起来像 bug。

### 前端要点

- `src/api/home.js`、`src/api/search.js`（都带后端不可用时的兜底数据）
- `HomeView.vue`：首页展示（通报/统计/分类/三个书架）与检索结果二选一显示，
  由 `searched` 开关控制；首屏静默调一次 `runSearch(true)` 只为拿分面条数。
- 卷宗详情用 `Teleport to="body"` 的弹窗，支持 ESC 与点遮罩关闭。

---

## 5.5 登录页增强（2026-10-15）

改动文件：`src/views/LoginView.vue`（样式全部 `scoped`，不动 theme.css，不影响注册页/找回页）

- 互动效果：卡片跟随鼠标 3D 倾斜（`rotateX/rotateY` 最大 9°/7°）+ 光晕跟随
  （`--mx/--my` + `mix-blend-mode: screen`）、认证失败卡片抖动、认证通过金色扫描过场、
  密钥「血统共鸣度」进度条（长度 + 字符种类打分）
- 便利功能：大写锁定提示（`e.getModifierState('CapsLock')`）、`Ctrl/⌘ + Enter` 提交、
  首空字段自动聚焦、上次登录时间（`localStorage['dr:last-login']`）、一键清除本机记录、
  后端离线时「重新检测」、连续 5 次失败 → 安全锁 30 秒倒计时（成功后清零，刷新即解）
- 无障碍：动效遵循 `prefers-reduced-motion`；触屏（`pointerType === 'touch'`）不启用倾斜
- 踩坑：卡片 3D 与抖动动画**必须挂在嵌套的两层 div 上** —— 外层 `perspective` + 抖动 keyframes，
  内层 `transform: rotateX/rotateY()`。挂在同一元素上时，keyframes 的 `transform` 会覆盖倾斜的 `transform`
- 监听器（caps / 快捷键）在 `onBeforeUnmount` 里统一移除，冷却定时器也要 `clearInterval`

## 5.6 登录页 / 注册页二次增强（2026-10-15）

新增组件（两个页面共用）：

- `src/components/IdCardPreview.vue` —— 身份卡实时预览。state: `idle | scan | ok`；
  编号派生「条形码」（字符串 hash，同一编号图案恒定）；`ok` 状态金色描边 + 循环下落的扫描纹。
- `src/components/OtpInput.vue` —— 六格验证码输入器。逐格输入自动跳格、退格回跳、整串粘贴自动拆分、
  方向键移动光标、填满抛 `complete`、`defineExpose({ focus })`。
  踩坑：格子必须 `maxlength="1"` + 聚焦时 `setSelectionRange(0,1)`（全选覆盖），
  否则「格内已有值再输入」会卡住；`onInput` 里要手动 `el.value = digits[i]` 把值同步回 DOM，
  否则 Vue 因状态未变不会重渲染，格子会残留两位数字。

登录页 `LoginView.vue` 新增：

- 顶部身份卡预览：随编号/密钥实时刷新（空 = 待刷卡，有编号 = 读取中，成功 = 已签发）
- 编号自动规整：`watch` 去空格 + 转大写
- 本机历史身份卡芯片：最多 4 个（`localStorage['dr:accounts']`），点击一键填充，× 可删除
- 密钥「能量格」6 格随长度点亮（叠加在原有共鸣度进度条之上）
- 后端延迟探测：ping（ms）+ 三格信号强度条
- 认证通过后打字机欢迎语（42ms/字）带闪烁光标；跳转延时 1s → 1.6s
- 账号框按 Enter 自动跳到密码框；登录成功写 `dr:name-of-<编号>`，下次刷卡显示代号
- `utils/auth.js` 新增 `historyAccounts / pushHistoryAccount / removeHistoryAccount / clearHistoryAccounts`

注册页 `RegisterView.vue` 改为三步向导：

- 步骤：1 身份核验（代号+身份+验证码）→ 2 血统鉴定（言灵卡片）→ 3 密钥设定（密钥/确认/协议）
- 用上了 theme.css 里一直闲置的 `.stepper / .stepper__dot / .stepper__line` 样式 + 顶部进度条
- 言灵由 `select` 改为可视化卡片网格（图标 + 说明 + 选中金色勾），第 2 步必须手动点选一次
- 验证码改为 OtpInput 六格；系统给出的金色验证码**可点击一键填入**
- 「让学院为我生成密钥」：crypto 随机 12 位（大小写+数字+符号，洗牌）+ 一键复制
- 第 3 步不再禁用提交按钮，改由 `onSubmit` 全量校验后给具体错误，并自动回退到出错的步骤
- 提交成功播放「已签发」印章过场（旋转缩放 + 扩散光环）
- 全程 `Ctrl/⌘ + Enter` = 进入下一步 / 提交

注意：登录卡片变高后，视口低于约 900px 时需要滚动，属正常。

## 6. 演示账号（数据库初始化后可用）

密钥统一为 `Dragon@2026`：

| 学院编号 | 姓名 | 邮箱 | 身份 | 言灵 |
| --- | --- | --- | --- | --- |
| `CC20260001` | 路明非 | luffy.cassell@cassell.edu | freshman | junyan |
| `CC20260002` | 楚子航 | chu.zihang@cassell.edu | executor | shiling |
| `CC20260003` | 芬格尔 | finger@cassell.edu | student | fengwang |

---

## 7. 快速启动（重置后照此执行）

### 最省事：双击 `启动.bat`

它会自动完成：检查环境 → 检查/初始化数据库 → 释放端口 5177/8082 →
`npm install` → 后端 `mvnw.cmd spring-boot:run` → 前端 `npm run dev` → 打开浏览器。

配套脚本（全部 **GBK 编码、相对路径、双击即用，且已实测通过**）：

| 脚本 | 作用 | 验证状态 |
| --- | --- | --- |
| `启动.bat` | 一键启动（自动检查环境、自动探测 MySQL 版本、初始化库、清端口、npm install、起后端与前端、开浏览器；后端已强制 `--server.port=8082`） | 2026-10-15 实测：8082 + 5177 均起、代理通 |
| `停止.bat` | 按端口精准杀掉 5177 / 8082 上的进程 | 已实测退出码 0 |
| `重启.bat` | 先停止再启动 | 逻辑已验证 |
| `初始化数据库.bat` | 建库建表 + 灌 3 个演示账号（幂等，可重复跑） | 已实测退出码 0，中文正常 |

> ⚠️ **不要随意跑 `初始化数据库.bat`**：`sql/dragon_raja.sql` 里有 `DROP TABLE IF EXISTS dr_user`，
> 重跑会**清掉所有已注册账号**（只剩 3 个演示账号）。库里已有数据时，它会被 `启动.bat` 自动跳过。

> 后端窗口若启动失败，请看那个窗口里的报错；
> 若提示找不到数据库，多半是 MySQL 没启动 —— 先确认 3306 端口在监听。

### 手动启动（想看每步输出时）

```bash
# 终端 1 —— 后端（必须用 mvnw.cmd，没有全局 mvn）
cd ../springBootTest     # 后端是前端的兄弟目录
set "SERVER__PORT="      # 关键！不清掉会被劫持到随机端口，见 §1 坑 3
set "SERVER__HOST="
mvnw.cmd spring-boot:run -Dspring-boot.run.arguments=--server.port=8082
# 看到 "Started SpringBootTestApplication" 即成功，端口 8082

# 终端 2 —— 前端
cd DragonRaja-Vue
npm install          # node_modules 已被删，必须先装
npm run dev          # 端口 5177
```

启动后访问 **http://127.0.0.1:5177**

---

## 8. 常见故障速查

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| 后端报 `Port 56xxx/64xxx was already in use`（端口是随机数，不是 8081） | 环境注入 `SERVER__PORT` 劫持了 `server.port` | 见 §1 坑 3：清 `SERVER__PORT`，或直接加 `-Dspring-boot.run.arguments=--server.port=8081` |
| 双击 `启动.bat` 一闪而过，提示 `npm install 失败`（其实网络没问题） | MySQL 装在 8.4，脚本写死 8.0 路径 → `NO_MYSQL` 落到了 `:NPM_FAIL` | 见 §1 坑 4（新脚本已修复） |
| 登录报 `code:500 服务器开小差了` | `dragon_raja` 库不存在 | 跑 `初始化数据库.bat`，或按 §1 手工执行 SQL |
| 后端启动即失败 / 连不上库 | 同上 | 同上 |
| `/api/health` 说已连接但登录仍失败 | health 文案写死，误导 | 忽略 health，直接看登录接口 |
| `ERROR 1366 Incorrect string value` | SQL 缺 `--default-character-set=utf8mb4` | 加上该参数重跑 |
| bat 报 `此时不应有 ...` 后静默退出 | GBK 下 echo 含半角括号，或 if 块内做重定向 | 见 §1.5，用全角括号 + goto 标签 |
| bat 里中文显示成 `鏁版嵁` | MySQL 用 utf8mb4 输出但控制台是 936 | 展示查询改用 `--default-character-set=gbk` |
| 前端 5177 端口被占 | 上次没关干净 | 跑 `停止.bat` |
| 后端 8082 端口被占 | 同上（或有僵尸 java 进程） | 跑 `停止.bat` |
| `mvn` 不是内部或外部命令 | 本机无全局 Maven | 用 `mvnw.cmd`（脚本已处理） |
| 端口被占但 `停止.bat` 没杀掉 | 进程以其他用户/权限运行 | 用**管理员身份**运行 `停止.bat` |
| 想换端口 | 前端改 `vite.config.js` 的 `server.port`；后端改 `application-dev.properties` 的 `server.port`，**两边要同步** | 代理目标可用 `VITE_API_TARGET` 覆盖 |

---

## 8.5 档案库相关故障速查

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| 首页一片空白、只有导航 | `dr_archive*` 表不存在 | 跑 `初始化档案库.bat` |
| `SQLSyntaxErrorException` 且报错里出现 `= 1 GROUP BY` | 非 `<script>` 的 `@Select` 里写了 `&lt;=` | 见「坑 7」 |
| 同一个身份一会儿能看到绝密卷宗、一会儿看不到 | 请求没带 `Authorization`，走的是游客权限 | 前端 `request()` 要传 `auth: true` |
| 一级分类条数和二级分类条数对不上 | 分面口径不一致 | 见第 5.3 节「分面统计口径一致」 |

---

## 9. 给 AI 的行为约定

1. **先读本文件**，别从头遍历代码库。
2. 改配置只动 `springBootTest/src/main/resources/`，**不要动 `target/classes/`**（会被删）。
3. 中文相关的 MySQL 操作**一律加 `--default-character-set=utf8mb4`**。
4. 验证后端是否可用，**不要信 `/api/health`**，请调 `/api/auth/login` 或查库。
5. 脚本必须用**相对路径**（`%~dp0`），且**不得依赖已存在的 `node_modules` / `target`**——它们每次重置都会被删。
6. 新增 `.bat` 必须存为 **GBK(936) 编码**，否则中文乱码。
7. 需要长时间运行的服务用后台窗口启动，不要让 AI 进程阻塞。

---

*最后更新：2026-10-15（三）· 本轮变更：新增档案库 5 张表 + 首页展示（通报/统计/两级分类/精选·热门·最新）+ 分级分类检索（关键词+两级分类+密级分级+排序+分页+分面统计）+ 卷宗详情弹窗；后端新增 OptionalAuthInterceptor 软鉴权与 SecurityLevel 密级体系*

> **上一条（同日早些时候）**：项目迁至 Desktop\Dragon（前后端平级）、首页 /home 上线（导航栏+搜索栏，/api/search 预留）、登录改纯学号体系、脚本后端路径改 `..\springBootTest`