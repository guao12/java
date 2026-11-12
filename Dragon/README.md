# 龙族读者社区（统一工程）

卡塞尔学院主题的龙族同人社区：前端 SPA + 后端 REST，前后端分离、同仓库协作。

```
浏览器 ──(5177)──> DragonRaja-Vue（Vue 3 + Vite）
                        │  /api 反向代理
                        ▼
                   springBootTest（Spring Boot 3.4.5）──> MySQL 8（dragon_raja）
```

## 快速开始

### 方式一：双击（推荐）

```
一键启动.bat
```

自动检查端口 → 装前端依赖 → 后端有 jar 且比源码新就直接跑，否则先打包 → 起前后端 → 打开浏览器。

### 方式二：手动

```bash
# 0. 前置：MySQL 8 已启动，且已导入数据库（首次必做）
#    先跑 springBootTest/sql/dragon_raja.sql，再跑 dragon_raja_admin.sql、dragon_raja_archive.sql

# 1. 后端（默认 dev profile，端口 8082）
cd springBootTest
mvnw.cmd spring-boot:run        # 或在 IDEA 里跑 SpringBootTestApplication

# 2. 前端
cd DragonRaja-Vue
npm install
npm run dev      # http://192.168.68.1:5177
```

### 打开哪一页

| 地址 | 说明 |
| --- | --- |
| `http://192.168.68.1:5177/` | 落地页（徽记首屏 → 序章 → 门户） |
| `/portal` | 学院门户（双阵营 + 公告 + 登录） |
| `/login`、`/register` | 身份认证 / 新生登记 |
| `/forum` | 讨论区（主站，游客可读） |
| `/archive` | 档案室（需登录，按密级解锁） |
| `/admin/login` | 后台管理（独立管理员令牌） |

演示账号：`CC20260001` / `Dragon@2026`

## 目录

| 路径 | 内容 |
| --- | --- |
| `springBootTest/` | 后端：13 个 Controller、17 张表、JWT + 软鉴权；`sql/` 内是全量建库脚本 |
| `DragonRaja-Vue/` | 前端：18 个视图、请求层、设计令牌；`启动.bat` 是前端的完整启动器 |
| `整合方案.md` | 原 `longzu` 单页原型并入本工程的方案、冲突处置与验收清单 |
| `一键启动.bat` | 根级统一入口 |

## 环境要求

| 依赖 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 17+ | 实测 Zulu 21 可用 |
| Node.js | 18+ | 实测 22.x |
| MySQL | 8.0 | 库名 `dragon_raja`，账号 root / 123456（可用 `DR_DB_URL` 等环境变量覆盖） |

## 配置要点

后端 `dr.jwt.secret` **没有默认值**，必须从环境变量注入：

- 双击脚本启动：自动读 `springBootTest/.run-secret`
- 命令行 / IDEA：`set DR_JWT_SECRET=<32 字节以上的密钥>`（dev profile 有兜底值，缺了也能启动）

数据源默认连 `192.168.68.1:3306`。若报 `Host 'xxx' is not allowed to connect`，
说明 MySQL 只授权了 `root@localhost`，要么改连 `127.0.0.1`：

```bash
set DR_DB_URL=jdbc:mysql://127.0.0.1:3306/dragon_raja?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
```

要么在 MySQL 里补授权：

```sql
CREATE USER 'root'@'%' IDENTIFIED BY '123456';
GRANT ALL PRIVILEGES ON dragon_raja.* TO 'root'@'%';
FLUSH PRIVILEGES;
```

> 注意：当前 shell 里若有 `SERVER__PORT` 环境变量，Spring 的宽松绑定会覆盖 `server.port`，
> 导致「配置写 8082、实际跑在随机端口」。启动脚本已清空该变量。

## 子文档

- 后端：`springBootTest/README.md`
- 前端：`DragonRaja-Vue/README.md`（功能清单、接口表、自定义指南）
- 前端开发备忘：`DragonRaja-Vue/AI-MEMO.md`（踩过的坑、环境现状）
- 整合记录：`整合方案.md`

---

> 粉丝向演示作品，画面与文案为原创演绎，与原著版权方无关。
