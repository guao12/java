# springBootTest —— 假面骑士创骑 · 骑士系统后端

把原来的 Thymeleaf 单体 Demo 改造成 **前后端分离** 的 REST 后端：
Spring Boot 3.4.5 + MyBatis-Plus 3.5.9 + MySQL 8，前端为独立 Vue 3 项目（Vite）。

---

## 一、技术栈

| 层次 | 技术 | 说明 |
|---|---|---|
| 框架 | Spring Boot 3.4.5 | 需要 JDK 17+（本机 JDK 21） |
| Web | spring-boot-starter-web | REST 接口，返回 JSON |
| 持久层 | MyBatis-Plus 3.5.9 | `mybatis-plus-spring-boot3-starter`（注意是 boot3 版本） |
| 数据库 | MySQL 8.0 | 库名 `kr_build`，账号 root / 123456 |
| 连接池 | HikariCP | MyBatis-Plus starter 自带，无需额外依赖 |
| 参数校验 | spring-boot-starter-validation | `@Valid` + `@NotBlank` / `@Email` |
| 密码加密 | spring-security-crypto | 只引 BCrypt 工具，不引整套 Security 过滤器链 |

> 原 `spring-boot-starter-thymeleaf` 与 `templates/` 已移除：页面交给 Vue，后端只出接口。

---

## 二、目录结构

```
src/main/java/com/fenglin/springboottest
├── SpringBootTestApplication.java   启动类（启动后打印接口地址）
├── common/
│   ├── R.java                       统一响应体 {code, message, data}
│   ├── BizException.java            业务异常
│   ├── GlobalExceptionHandler.java  全局异常 → 统一 JSON
│   └── SessionConst.java            Session 常量（登录态 key）
├── config/
│   ├── AppConfig.java               PasswordEncoder(BCrypt) Bean
│   ├── MybatisPlusConfig.java       @MapperScan + 分页/乐观锁等插件
│   └── WebMvcConfig.java            CORS 跨域
├── controller/
│   ├── UserController.java          /api/user/**   登录、注册、退出、花名册
│   └── HealthController.java        /api/health    健康检查
├── dto/     LoginDTO / RegisterDTO  接收前端参数
├── entity/  User                    对应 kr_user 表
├── mapper/  UserMapper              extends BaseMapper<User>
├── service/ UserService + impl      业务逻辑
└── vo/      UserVO                  返回给前端的对象（不含 password）

src/main/resources
├── application.properties           公共配置（profile、MyBatis-Plus、日志）
├── application-dev.properties       开发环境：端口 8081 + 数据源
├── application-test.properties      测试环境：端口 8080 + kr_build_test 库
├── application-prod.properties      生产环境：端口 8082 + 环境变量注入密码
├── db/init.sql   ← 实际位于项目根目录 db/init.sql
└── static/index.html                后端自带的接口导航页

db/init.sql                          建库建表脚本（kr_build / kr_build_test）
```

---

## 三、启动步骤

### 1. 建库建表

```bash
# 命令行方式
mysql -uroot -p123456 < db/init.sql

# 或在 IDEA 的 Database 工具窗口打开 db/init.sql 直接执行
```

会自动创建 `kr_build`、`kr_build_test` 两个库和 `kr_user` 表。

### 2. 启动后端

IDEA 里直接 Run `SpringBootTestApplication` 即可，默认激活 `dev`（端口 **8081**）。

命令行方式（本机没配 `mvn` 到 PATH 时）：

```bash
# 使用 .m2 里已有的 maven
"C:\Users\Administrator\.m2\wrapper\dists\apache-maven-3.9.9-bin\4nf9hui3q3djbarqar9g711ggc\apache-maven-3.9.9\bin\mvn.cmd" clean package
java -jar target/springBootTest-0.0.1-SNAPSHOT.jar
```

验证：

- 接口导航页 <http://localhost:8081/index.html>
- 健康检查 <http://localhost:8081/api/health>

```json
{"code":200,"message":"ok","data":{"app":"springBootTest","profile":"dev","port":"8081",
 "database":"MySQL 已连接","userCount":0,"time":"2026-10-08 14:23:04"}}
```

### 3. 启动前端

```bash
cd C:\Users\Administrator\Desktop\创骑文件夹
npm install     # 首次
npm run dev
```

打开 <http://localhost:5173>。前端通过 Vite 代理把 `/api/**` 转发到 `http://localhost:8081`，
所以**不存在跨域问题，也不用改前端请求地址**。后端端口若变更，同步改 `vite.config.js` 的 `BACKEND`。

---

## 四、接口清单

统一响应体：

```json
{ "code": 200, "message": "ok", "data": { } }
```

`code`：200 成功 / 400 业务或参数错误 / 401 未登录 / 500 服务器异常。
前端只需判断 `code === 200`。

| 方法 | 地址 | 说明 | 请求体 / 参数 |
|---|---|---|---|
| GET | `/api/health` | 健康检查（含数据库连通性、用户数） | - |
| POST | `/api/user/register` | 注册 | `{username, email, password, confirmPassword, formKey}` |
| POST | `/api/user/login` | 登录，成功后写入 Session | `{username, password, remember}` |
| POST | `/api/user/logout` | 退出登录 | - |
| GET | `/api/user/me` | 当前登录用户 | 依赖 Cookie 里的 Session |
| GET | `/api/user/check-username` | 骑士名是否可用 | `?username=xxx` |
| GET | `/api/user/list` | 骑士花名册（最多 100 条，不含密码） | - |

登录态方案：**Session + Cookie**（`JSESSIONID`）。前端 `fetch` 已带 `credentials: 'include'`，
Vite 代理也默认转发 Cookie，因此开箱即用。若后续要前后端分域部署，可换成 JWT。

---

## 五、数据库表 `kr_user`

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键，自增 |
| username | varchar(50) | 骑士名，**唯一** |
| email | varchar(100) | 邮箱，**唯一** |
| password | varchar(100) | BCrypt 密文（60 字符，形如 `$2a$10$...`） |
| form_key | varchar(20) | 当前形态：rabbittank / hazard / genius |
| status | tinyint | 1 正常 / 0 禁用 |
| deleted | tinyint | 逻辑删除：0 未删除 / 1 已删除（MyBatis-Plus 自动过滤） |
| create_time | datetime | 创建时间，数据库默认值 |
| update_time | datetime | 更新时间，`ON UPDATE` 自动刷新 |

MyBatis-Plus 相关配置见 `application.properties`：驼峰转换、逻辑删除、主键策略、SQL 日志。

---

## 六、常见问题

**1. 启动报 `Access denied for user 'root'@'localhost'`**
检查 `application-dev.properties` 里的账号密码是否与本地 MySQL 一致。

**2. 中文乱码**
数据库/表已用 `utf8mb4`，JDBC URL 带 `characterEncoding=utf8`。
若 IDEA 里 `.properties` 中文注释乱码：`Settings → Editor → File Encodings` 全部设为 **UTF-8**。

**3. `Public Key Retrieval is not allowed`**
JDBC URL 已带 `allowPublicKeyRetrieval=true`，无需处理。

**4. 端口被占用 / 想换端口**
改 `application-dev.properties` 的 `server.port`，同时改前端 `vite.config.js` 的 `BACKEND`。

**5. 想加分页**
引入 `mybatis-plus-jsqlparser` 依赖后，在 `MybatisPlusConfig` 注册 `PaginationInnerInterceptor` 即可
（3.5.9 起分页插件被拆到该独立包，不再是核心包自带）。
