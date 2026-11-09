# springBootTest —— 卡塞尔学院 · 龙族身份认证系统后端

前后端分离的 REST 后端：**Spring Boot 3.4.5 + MyBatis-Plus 3.5.9 + MySQL 8 + JWT**，
对接前端独立 Vue 3 项目 **DragonRaja-Vue**（龙族主题登录 / 注册 / 密钥重置 / 学院内网）。

---

## 一、技术栈

| 层次 | 技术 | 说明 |
|---|---|---|
| 框架 | Spring Boot 3.4.5 | 需要 JDK 17+ |
| Web | spring-boot-starter-web | REST 接口，返回 JSON |
| 持久层 | MyBatis-Plus 3.5.9 | `mybatis-plus-spring-boot3-starter`（注意是 boot3 版本） |
| 数据库 | MySQL 8.0 | 库名 `dragon_raja`，账号 root / 123456 |
| 连接池 | HikariCP | MyBatis-Plus starter 自带 |
| 参数校验 | spring-boot-starter-validation | `@Valid` + `@NotBlank` / `@Pattern` |
| 密码加密 | spring-security-crypto | 只引 BCrypt 工具，不引整套 Security 过滤器链 |
| 鉴权 | jjwt 0.12.6（HS256） | 无状态 JWT，前端 `Authorization: Bearer <token>` |

---

## 二、目录结构

```
src/main/java/com/fenglin/springboottest
├── SpringBootTestApplication.java   启动类
├── common/
│   ├── R.java                       统一响应体 {code, message, data}，code==0 成功
│   ├── BizException.java            业务异常
│   ├── CurrentUser.java             ThreadLocal 登录上下文（拦截器写入）
│   └── GlobalExceptionHandler.java  全局异常 → 统一 JSON
├── config/
│   ├── AppConfig.java               PasswordEncoder(BCrypt) Bean
│   ├── JwtInterceptor.java          JWT 拦截器（拦 /api/user/**，401 返回 JSON）
│   ├── MybatisPlusConfig.java       @MapperScan 等
│   └── WebMvcConfig.java            CORS + 拦截器注册
├── controller/
│   ├── AuthController.java          /api/auth/**  注册、登录、账号探测、密钥重置、字典
│   ├── ProfileController.java       /api/user/**  身份卡、退出登录（JWT）
│   └── HealthController.java        /api/health   健康检查
├── dto/     RegisterDTO / LoginDTO / ResetSendDTO / ResetConfirmDTO
├── entity/  DrUser(dr_user) / DrResetCode(dr_reset_code)
├── mapper/  DrUserMapper / DrResetCodeMapper（BaseMapper + 注解 SQL）
├── service/ AuthService / DictService / TokenService + impl
└── vo/      ProfileVO / LoginVO / RegisterVO / DictVO / CheckAccountVO / SendCodeVO

sql/dragon_raja.sql                  全量建库建表脚本（14 张表 + 演示数据，含 DROP，只跑一次）
sql/dragon_raja_admin.sql            增量：管理员 dr_admin + 言灵图鉴 dr_spirit（CREATE IF NOT EXISTS）
sql/dragon_raja_archive.sql          增量：档案库 5 张表 + 演示卷宗（CREATE IF NOT EXISTS）
```

---

## 三、启动步骤

### 1. 建库建表（本地 MySQL root / 123456）

```bash
# 命令行方式（版本号换成你本机装的目录：8.0 / 8.4 / …）
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -p123456 < sql/dragon_raja.sql

# 或在 IDEA 的 Database 工具窗口打开 sql/dragon_raja.sql 直接执行
```

> 脚本自带 `CREATE DATABASE IF NOT EXISTS dragon_raja` + `USE dragon_raja`，
> 不需要事先手动建库，也不需要指定库名。
> 双击 `DragonRaja-Vue\初始化数据库.bat` 效果相同（该脚本会自动探测 mysql.exe 的安装版本）。

会创建 `dragon_raja` 库、14 张表（`dr_user` / `dr_reset_code` / `dr_admin` / `dr_spirit` /
`dr_archive*` / `dr_board` / `dr_thread` / `dr_reply` / `dr_thread_action` / `dr_notice` /
`dr_hot_keyword` / `dr_search_log`），并写入演示账号（密钥统一 `Dragon@2026`）。

> 任务系统下线后，全量 dump 里已不再包含 `dr_user_clearance`（2026-11 起）。

> ⚠️ 该脚本含 `DROP TABLE`，重跑会清空已注册账号。**建库只需执行一次。**
> `dr_user` 上有 `uk_account` + `uk_codename` 两个唯一索引，并发注册同名代号由唯一键兜底。

**已存在的库（2026-11-06 之前建的）不会自动获得 `uk_codename`**，需单独补一次：

```sql
-- 先确认没有重复代号；有结果就先人工合并，否则加索引会失败
SELECT codename, COUNT(*) FROM dr_user WHERE deleted = 0 GROUP BY codename HAVING COUNT(*) > 1;

ALTER TABLE `dr_user` ADD UNIQUE INDEX `uk_codename`(`codename` ASC) USING BTREE;
```

### 1.5 建档案库（首页展示 + 分级分类检索）

```bash
# 版本号换成你本机装的目录
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -p123456 < sql/dragon_raja_archive.sql

# 或双击 DragonRaja-Vue\初始化档案库.bat
```

会创建 `dr_archive_category` / `dr_archive` / `dr_notice` / `dr_hot_keyword` / `dr_search_log`
五张表，并写入 21 个分类、35 条演示卷宗、3 条公告、8 个热词。

**全部是 `CREATE TABLE IF NOT EXISTS` + `INSERT IGNORE`，不含任何 `DROP`，可放心重复执行**，
且完全不动 `dr_user`。

### 1.6 任务系统与权限点（已下线）

**任务系统已在 2026-11 移除**：前端不再有「执行部任务」页（旧的 `/missions` 会重定向到 `/archive`），
档案馆的五卷对所有人无条件开放，密级（公开 / 内部 / 机密 / 绝密）只作为卷宗的分级标注保留。

配套变化：

- `utils/clearance.js` 只剩 `LEVELS` 密级标签；`completeMission` / 权限点 / 备份码 / 后端同步全部删除；
- 后端 `ClearanceController`、`ClearanceService(+Impl)`、`DrUserClearance`、`DrUserClearanceMapper`、
  `ClearanceVO` 与 `db/clearance-schema.sql` 已删除，`spring.sql.init.schema-locations` 里也去掉了它；
  `GET/PUT /api/user/clearance` 现在返回 404。旧库里若还留着 `dr_user_clearance` 表不影响运行，
  确认不需要历史数据后可以 `DROP TABLE dr_user_clearance;`；
- `ForumOverviewVO.Me` 不再返回 `points` / `levelLabel`，`BoardVO` 不再返回 `minPoints`；
  版块准入只看在校身份（`canReadBoard` 已去掉权限点判断），执行部内网的 `min_points` 置 0；
- `dr_board.min_points` 列仍留在库里（历史字段，代码不再读写），新库/种子脚本也不再用它；
- 论坛首页侧栏的「积分 / 每日任务」入口替换为「档案馆 · 查阅卷宗」。

> 论文/演示时若要恢复该玩法，需要同时恢复任务页、权限点来源、后端 clearance 模块与
> `hasClearance` 判定，只把路由挂回来是不够的。

### 1.7 跨电脑访问

只想让同一局域网的另一台电脑打开站点时，**共用一套后端即可**：只在主机上跑 MySQL + 8082，
另一台浏览器打开 `http://<主机IP>:5177`（`vite.config.js` 的 dev server 已监听 `0.0.0.0`，
`/api` 由主机代理到本机 8082）。只想本机访问时用 `VITE_HOST=127.0.0.1 npm run dev`。

### 1.8 配置 JWT 密钥（必做，漏了会启动失败）

`application.properties` 里是 `dr.jwt.secret=${DR_JWT_SECRET}`，**故意没有默认值**，
不注入就会抛 `Could not resolve placeholder 'DR_JWT_SECRET'` 直接退出。

项目根目录放一个 `.run-secret`（单行、64 字符、结尾不要带多余空格），
本目录的 `run-backend.bat` 与 `../DragonRaja-Vue/启动.bat` 都会自动读取它：

```bash
# 生成一个（Git Bash / Linux）
openssl rand -base64 48 | tr -d '\n' > .run-secret
```

手动命令行的等价写法：

```bash
DR_JWT_SECRET=$(cat .run-secret) java -jar target/springBootTest-0.0.1-SNAPSHOT.jar
```

> 换密钥后之前签发的 JWT 全部失效，前端需要重新登录。

### 2. 启动后端

IDEA 里直接 Run `SpringBootTestApplication`，默认激活 `dev`（端口 **8082**）。
**记得在 Run Configuration 的 Environment variables 里加上 `DR_JWT_SECRET`**（见 1.8）。

命令行方式（本机没配 `mvn` 到 PATH 时用项目自带 wrapper）：

```bash
mvnw.cmd clean package -DskipTests
java -jar target/springBootTest-0.0.1-SNAPSHOT.jar
```

> 若 `mvnw` 报 `ClassNotFoundException: org.codehaus.plexus.classworlds.launcher.Launcher`
> （Git Bash / MinGW 下路径转换导致），改用 `sh ./mvn-run.sh package -DskipTests`，
> 它直接调 `java -classpath boot/plexus-classworlds-*.jar ... Launcher` 绕过该问题，
> 并会自动匹配 `~/.m2/wrapper/dists` 下实际存在的 Maven 版本。

最省事的方式：直接双击本目录的 **`run-backend.bat`**，自动读 `.run-secret` 并固定 8082 端口。

#### 开发模式（改完代码自动重启）

改代码频繁时**不要用 `java -jar`**：`spring-boot-maven-plugin` 的 `repackage` 默认
`excludeDevtools=true`，devtools 根本不在 fat jar 里，热重启永远不触发；
而且每次都要 `package` 一次，`clean` 还会把 `target/` 删掉、正在跑的 JVM 直接消失。

开发模式请双击 **`dev-run.bat`**（等价于 `mvn spring-boot:run`）：

- 从 `.run-secret` 读密钥，自动探测 `~/.m2/wrapper/dists` 下的 Maven 发行版
- devtools 生效：LiveReload 监听 **35729**，改完代码触发一次编译后进程自动重启
- `dev-run.bat --check` 只打印探测到的 Maven / plexus / java / 密钥状态，不启动，便于排查

> 热重启时如果撞上 IDE 正在全量编译，可能在 `target/classes` 处于半成品的那一瞬间
> 报 `No qualifying bean of type ...` 之类的启动失败 —— 编译结束后会自动再重启一次，
> 属于瞬时现象，不是代码问题。

验证：

- 接口导航页 <http://192.168.68.1:8082/index.html>
- 健康检查 <http://192.168.68.1:8082/api/health>（`code == 0` 即正常）

### 3. 启动前端

```bash
cd C:\Users\guao\Desktop\java-main\DragonRaja-Vue
npm install     # 首次
npm run dev     # http://192.168.68.1:5177
```

> Vite 默认绑定 `192.168.68.1`（2026-11-09 由 `0.0.0.0` 切换），启动后直接访问
> `http://192.168.68.1:5177` 即可。
> `DragonRaja-Vue\启动.bat` 也会在「启动完成」里打印局域网地址 —— 多电脑共用时，
> 其它电脑打开这个地址即可，
> 不要再在那台机器上跑一遍 `启动.bat`（否则连的是它自己那套 MySQL，权限进度会读不到，
> 详见 1.6 的「跨电脑同步的前提」）。想让 127.0.0.1 也能访问时用 `VITE_HOST=0.0.0.0 npm run dev`。

前端通过 Vite 代理把 `/api/**` 转发到 `http://192.168.68.1:8082`，
**不存在跨域问题，也不用改前端请求地址**。后端端口若变更，同步改 `vite.config.js` 的 `VITE_API_TARGET`。

演示账号：`CC20260001` + `Dragon@2026`。

---

## 四、接口清单

统一响应体（与前端 `src/api/http.js` 约定一致）：

```json
{ "code": 0, "message": "ok", "data": { } }
```

`code`：**0 成功** / 400 业务或参数错误 / 401 未登录或令牌失效 / 500 服务器异常。

| 方法 | 地址 | 说明 | 请求体 / 参数 |
|---|---|---|---|
| GET | `/api/health` | 健康检查（含数据库连通性、用户数） | - |
| POST | `/api/auth/register/send` | 下发注册验证码（演示环境直接回显，不发邮件） | `{clientKey}` |
| POST | `/api/auth/register` | 新生登记（需先通过验证码校验），签发学号 CC2026xxxxx | `{codename, identity, spirit, password, code, clientKey}` |
| POST | `/api/auth/login` | 身份认证，返回 JWT + 身份卡 | `{account, password, remember}` |
| GET | `/api/auth/check-account` | 账号是否已登记（返回行动代号） | `?account=学号` |
| POST | `/api/auth/reset/send` | 下发 6 位验证龙文（5 分钟有效，60 秒冷却） | `{account}` |
| POST | `/api/auth/reset/confirm` | 校验验证龙文并重置密钥 | `{account, code, password}` |
| GET | `/api/auth/dictionaries` | 在校身份 / 言灵字典（前后端共用 code） | - |
| GET | `/api/user/profile` | 我的身份卡 | 需 `Authorization: Bearer <token>` |
| POST | `/api/user/logout` | 退出登录 | 需 `Authorization: Bearer <token>` |
| GET | `/api/home/overview` | 首页概览（通报 / 统计 / 两级分类 / 精选 · 热门 · 最新 / 热词） | 软鉴权 |
| GET | `/api/search` | 分级分类检索 + 分页 + 分面统计 | `?keyword=&topCategory=&category=&level=&sort=&page=&size=` |
| GET | `/api/search/categories` | 两级分类树（带可见卷宗数） | 软鉴权 |
| GET | `/api/search/hot` | 热门检索词 | 软鉴权 |
| GET | `/api/archive/{id}` | 卷宗详情（浏览量 +1，越级返回 400） | 软鉴权 |

登录态方案：**JWT（无状态）**。登录返回 `{token, expiresIn, user}`；
`/api/user/**` 由 `JwtInterceptor` 统一校验，未授权返回 HTTP 401 + `{code:401}`，
前端收到后自动清除本地会话并跳回登录页。

`/api/home/**`、`/api/search`、`/api/archive/**` 走 **`OptionalAuthInterceptor`（软鉴权）**：
带令牌就解析身份、不带也能访问，只是按「未认证访客」的公开级权限返回。

#### 好友与私信（`/api/friend/**`，全部需要登录）

好友关系存**双向两行**（A→B、B→A），查询只走单表唯一索引，解除时两行同删；
私信不建会话表 —— 一对一私聊的会话就是 `(sender_id, receiver_id)` 组合。
三张表（`dr_friend_request` / `dr_friendship` / `dr_message`）由 `db/friend-schema.sql`
随应用启动自动创建，换电脑不用手动跑脚本。

| 方法 | 地址 | 说明 |
|---|---|---|
| GET | `/api/friend/overview` | 概览：好友数 / 待处理申请 / 未读数 + 申请 + 好友列表（首屏一个请求） |
| GET | `/api/friend/badge` | 导航角标：未读私信数 / 待处理申请数（前端 20 秒轮询） |
| GET | `/api/friend/search?kw=` | 按学号或行动代号搜人，结果带关系状态（friend / pending_out / pending_in / none） |
| GET | `/api/friend/list` | 我的好友（按最近会话排序，含最后一条消息与未读数） |
| GET | `/api/friend/requests` | 我收到的待处理申请 |
| POST | `/api/friend/requests` | 发送申请 `{toUserId, message}`；**对方已向我申请时直接成为好友** |
| POST | `/api/friend/requests/{id}/accept` | 同意申请 |
| POST | `/api/friend/requests/{id}/reject` | 拒绝申请 |
| DELETE | `/api/friend/{userId}` | 解除好友（历史私信保留，重新加回还能看到） |
| GET | `/api/friend/messages?friendId=&beforeId=&size=` | 会话历史（时间正序，往上翻传 `beforeId`） |
| GET | `/api/friend/messages/new?friendId=&afterId=` | 增量拉新消息（前端 4 秒轮询只拉增量，不冲掉滚动位置） |
| POST | `/api/friend/messages` | 发私信 `{toUserId, content}`，1 秒内只能发一条 |
| POST | `/api/friend/read?friendId=` | 会话标记已读，返回剩余未读总数 |

> 只允许给好友发私信 —— 否则私信就是第二个垃圾广告通道。
> 拉取会话历史本身即视为已读，前端不需要额外调 `read`。

#### 档案密级分级（"分级"的含义）

| 密级 | 值 | 可见身份 |
|---|---|---|
| 公开 | 1 | 所有人（含未登录） |
| 内部 | 2 | freshman 新生 / student 在校生 |
| 机密 | 3 | executor 执行部 |
| 绝密 | 4 | teacher 教员 · 研究员 |

规则见 `common/SecurityLevel`。所有档案查询都带 `security_level <= maxLevel`：
越级卷宗不会出现在列表里，越级筛选会被收敛到可见上限，直接按 id 访问越级卷宗返回 400。

#### 分面统计（筛选项旁的条数）

`facetTopCategory` / `facetCategory` / `facetLevel` 三个维度的口径一致：
**每个维度只对「其他维度」的条件生效，忽略自身维度**，
所以「一级分类合计 == 二级分类合计 == 密级合计 == 命中总数」恒成立。

---

## 五、数据库表

### `dr_user`（混血种/用户表）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键，自增 |
| account | varchar(20) | 学院编号（学号），**唯一**，注册时后端签发 |
| codename | varchar(50) | 姓名 / 行动代号 |
| password | varchar(100) | BCrypt 密文 |
| identity | varchar(20) | 在校身份 code：freshman/student/executor/teacher/guest |
| spirit | varchar(20) | 言灵 code：none/junyan/bronze/shiling/fengwang/zhulong/wangquan |
| status | tinyint | 1 在册 / 0 封印（禁用） |
| deleted | tinyint | 逻辑删除，MyBatis-Plus 自动过滤 |
| login_count | int | 累计登录次数 |
| last_login_at | datetime | 最近登录时间 |
| create_time / update_time | datetime | 数据库默认值 / `ON UPDATE` 自动刷新 |

### `dr_reset_code`（密钥重置验证码表）

`account` + `code`（6 位数字）+ `expire_time` + `used`；重置成功后该账号所有验证码作废。

### `dr_archive_category`（档案分类表，两级树）

`parent_id = 0` 为一级分类（人物志 / 言灵谱 / 地理志 / 机构志 / 文献卷 / 制度典），
其余为二级分类。ID 由建表脚本固定分配（一级 1001+，二级 2001+），便于种子数据引用父子关系。

### `dr_archive`（档案主表）

| 字段 | 类型 | 说明 |
|---|---|---|
| code | varchar(40) | 卷宗编号，唯一（AR-P-001），关键词可搜 |
| title / summary / content | varchar / text | 标题 / 摘要（列表）/ 正文（仅详情返回） |
| top_category_id / category_id | bigint | 一级 / 二级分类（一级为冗余字段，便于大类筛选） |
| security_level | tinyint | **密级 1-4**，分级过滤的核心字段 |
| rank_label | varchar(20) | 评级标签：S / A / B / 龙王 / 禁咒 / 禁区 |
| tags | varchar(200) | 英文逗号分隔，参与关键词匹配 |
| view_count / heat | int | 浏览量 / 热度（排序用） |
| spotlight | tinyint | 首页精选 |

### `dr_notice`（首页公告）

`level` 1 普通 / 2 重要 / 3 紧急，`pinned` 置顶，`expire_at` 为空表示长期有效。

### `dr_hot_keyword` / `dr_search_log`（热词 / 检索日志）

每次检索写一条日志（`dr_search_log`），若关键词命中已有热词则热度 +1
—— 不自动收录新词，避免检索日志污染热词榜。

---

## 六、常见问题

**1. 启动报 `Access denied for user 'root'@'localhost'`**
检查 `application-dev.properties` 里的账号密码是否与本地 MySQL 一致（默认 root / 123456）。

**1.1 启动报 `Host 'xxx' is not allowed to connect to this MySQL server`**
数据源现在走 `192.168.68.1:3306`，而 MySQL 默认只建了 `root@localhost`。
在本机先补一个同 IP 的账号（只需一次）：

```sql
CREATE USER IF NOT EXISTS 'root'@'192.168.68.1' IDENTIFIED WITH mysql_native_password BY '123456';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'192.168.68.1';
FLUSH PRIVILEGES;
```

验证：`mysql -h 192.168.68.1 -uroot -p123456 -e "select current_user()"`。
不想要这个账号时：`DROP USER 'root'@'192.168.68.1';`

**2. 中文乱码**
数据库/表已用 `utf8mb4`，JDBC URL 带 `characterEncoding=utf8`。
若 IDEA 里 `.properties` 中文注释乱码：`Settings → Editor → File Encodings` 全部设为 **UTF-8**。

**3. `Public Key Retrieval is not allowed`**
JDBC URL 已带 `allowPublicKeyRetrieval=true`，无需处理。

**4. 端口被占用 / 想换端口**
改 `application-dev.properties` 的 `server.port`，同时改前端 `vite.config.js` 的代理 target。

**4.1 配置写的是 8082，启动后却跑在 8099 或某个随机端口**

这是**环境变量劫持**，不是配置写错。Spring Boot 的属性优先级里，操作系统环境变量高于
`application-*.properties`，而 `SERVER__PORT`（双下划线）/ `SERVER_PORT` 会被 relaxed binding
解析成 `server.port`。某些 IDE、终端或终端集成工具会往子进程里塞这个变量，于是：

```
2026-11-09 09:28:04 INFO  Tomcat initialized with port 8099 (http)   ← 配置里根本没有 8099
```

症状是**前端所有接口 500**：Vite 把 `/api` 代理到写死的 `8082`，而后端在 8099，
代理拿不到响应就返回 `500 text/plain`，前端再报「无法连接学院服务器（HTTP 500）」。

排查与解决：

```bat
rem 1) 看实际端口：启动日志里搜 Tomcat initialized with port，或看 banner 的「接口地址」
rem 2) 启动前清掉（或直接改回 8082）
set "SERVER__PORT=8082"
set "SERVER__HOST=0.0.0.0"
java -jar target\springBootTest-0.0.1-SNAPSHOT.jar --server.port=8082
```

`--server.port` 是**命令行参数，优先级最高**，能压过环境变量，所以启动命令里务必带上它。
仓库里的 `启动.bat`、`run-backend.bat`、`dev-run.bat` 都已经做了这一步（清变量 + 显式传参），
直接双击脚本启动就不会踩到。反过来，**手写 `java -jar xxx.jar` 不带参数**是这个问题最常见的触发方式。

**5. 想加分页**
引入 `mybatis-plus-jsqlparser` 依赖后，在 `MybatisPlusConfig` 注册 `PaginationInnerInterceptor` 即可
（3.5.9 起分页插件被拆到该独立包）。

**6. 换 JWT 密钥**
`dr.jwt.secret` 现在只接受环境变量 `DR_JWT_SECRET`（HS256 至少 32 字节），仓库里不写死默认值。
本地最省事的做法是重新生成 `.run-secret`（见 1.8），`run-backend.bat` 与前端 `启动.bat` 都会自动读取。
