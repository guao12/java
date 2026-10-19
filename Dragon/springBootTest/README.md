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
| 参数校验 | spring-boot-starter-validation | `@Valid` + `@NotBlank` / `@Email` / `@Pattern` |
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

sql/dragon_raja.sql                  建库建表脚本（dragon_raja / dragon_raja_test）
```

---

## 三、启动步骤

### 1. 建库建表（本地 MySQL root / 123456）

```bash
# 命令行方式（MySQL 8.4 示例路径）
"C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -uroot -p123456 < sql/dragon_raja.sql

# 或在 IDEA 的 Database 工具窗口打开 sql/dragon_raja.sql 直接执行
```

会创建 `dragon_raja`、`dragon_raja_test` 两个库，`dr_user` / `dr_reset_code` 两张表，
并写入 3 个演示账号（密钥统一 `Dragon@2026`）。

> ⚠️ 该脚本含 `DROP TABLE`，重跑会清空已注册账号。**建库只需执行一次。**

### 1.5 建档案库（首页展示 + 分级分类检索）

```bash
"C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" -uroot -p密码 < sql/dragon_raja_archive.sql

# 或双击 DragonRaja-Vue\初始化档案库.bat
```

会创建 `dr_archive_category` / `dr_archive` / `dr_notice` / `dr_hot_keyword` / `dr_search_log`
五张表，并写入 21 个分类、35 条演示卷宗、3 条公告、8 个热词。

**全部是 `CREATE TABLE IF NOT EXISTS` + `INSERT IGNORE`，不含任何 `DROP`，可放心重复执行**，
且完全不动 `dr_user`。

### 2. 启动后端

IDEA 里直接 Run `SpringBootTestApplication`，默认激活 `dev`（端口 **8082**）。

命令行方式（本机没配 `mvn` 到 PATH 时用项目自带 wrapper）：

```bash
mvnw.cmd clean package -DskipTests
java -jar target/springBootTest-0.0.1-SNAPSHOT.jar
```

验证：

- 接口导航页 <http://localhost:8082/index.html>
- 健康检查 <http://localhost:8082/api/health>（`code == 0` 即正常）

### 3. 启动前端

```bash
cd C:\Users\guao\Desktop\java-main\DragonRaja-Vue
npm install     # 首次
npm run dev     # http://127.0.0.1:5177
```

前端通过 Vite 代理把 `/api/**` 转发到 `http://127.0.0.1:8082`，
**不存在跨域问题，也不用改前端请求地址**。后端端口若变更，同步改 `vite.config.js` 的 `VITE_API_TARGET`。

演示账号：`CC20260001`（或 `luffy.cassell@cassell.edu`）+ `Dragon@2026`。

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

**2. 中文乱码**
数据库/表已用 `utf8mb4`，JDBC URL 带 `characterEncoding=utf8`。
若 IDEA 里 `.properties` 中文注释乱码：`Settings → Editor → File Encodings` 全部设为 **UTF-8**。

**3. `Public Key Retrieval is not allowed`**
JDBC URL 已带 `allowPublicKeyRetrieval=true`，无需处理。

**4. 端口被占用 / 想换端口**
改 `application-dev.properties` 的 `server.port`，同时改前端 `vite.config.js` 的代理 target。

**5. 想加分页**
引入 `mybatis-plus-jsqlparser` 依赖后，在 `MybatisPlusConfig` 注册 `PaginationInnerInterceptor` 即可
（3.5.9 起分页插件被拆到该独立包）。

**6. 换 JWT 密钥**
改 `application.properties` 的 `dr.jwt.secret`（HS256 至少 32 字节）；生产环境用环境变量 `DR_JWT_SECRET` 注入。
