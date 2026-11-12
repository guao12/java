# 卡塞尔学院 · 龙族身份认证系统（Vue 3）

龙族（Dragon Raja）同人风格的登录 / 注册页面，Vue 3 + Vite 单页应用。
已对接 **Spring Boot 3 + MyBatis-Plus + MySQL 8** 后端（同仓库 `..\springBootTest`），登录 / 注册 / 密钥重置均为真实接口。

## 端到端流程

```
浏览器 → Vite dev(5177) --代理 /api--> Spring Boot(8082) --MyBatis-Plus--> MySQL(dragon_raja)
```

1. 启动后端（默认 dev profile，端口 8082，需先执行后端项目的 `sql/dragon_raja.sql`）
2. `npm run dev` 启动前端，登录页底部会显示「后端通道 · 已连接（192.168.68.1:8082）」
3. 演示账号：`CC20260001`，密钥 `Dragon@2026`

## 特性

- **龙族主题视觉**：暗夜深渊底色 + 鎏金描边 + 玻璃拟态认证卡片
- **动态背景**：Canvas 悬浮龙文尘埃 / 上升流火粒子 + SVG 旋转龙文法阵 + 巨型「龍」水印
- **登录页**：学号或姓名校验、密钥显隐、记住身份卡、演示身份卡一键填充、后端连通状态指示
  - 互动效果：卡片跟随鼠标 3D 倾斜 + 光晕跟随、认证失败抖动、认证通过扫描过场、密钥「血统共鸣度」进度条
  - 便利功能：大写锁定提示、`Ctrl/⌘ + Enter` 快捷提交、首空字段自动聚焦、上次登录时间显示、一键清除本机记录、
    后端离线时「重新检测」、连续 5 次失败安全锁 30 秒倒计时（成功后清零，刷新即解）
  - 无障碍：全部动效遵循 `prefers-reduced-motion`，触屏不启用倾斜
- **注册页**：双列布局、身份与言灵下拉（下拉项来自后端字典接口）、**验证码由系统直接给出**（点「获取验证码」即回显 6 位码，5 分钟有效 / 60 秒冷却，不发邮件）、密码强度四档评估、协议勾选校验、提交后展示后端签发的学院编号并自动预填到登录页
- **密钥重置页 `/forgot`**：三步向导（验证身份 → 重置密钥 → 完成），验证龙文由后端下发（5 分钟有效 / 60 秒冷却）、重置后旧密钥失效
- **落地层 `/`**（2026-11 并入，来自原 `longzu` 单页原型）：访客首次进入的入口
  - 徽记首屏：院徽 + 三重轨道环 + 光晕脉冲 + 拂光扫过 + 入场动画（`motto` 题词）
  - **序章**：五段世界观逐字打字机 + 进度条，点击 / 空格 / 回车推进，ESC 跳过
  - **门户 `/portal`**：双阵营（卡塞尔学院 / 蛇岐八家）切换背景与文案、学院公告、登录与注册入口
  - 条件入口：已登录用户直接进 `/forum`；看过序章的回访用户直接到 `/portal`；`/?replay=1` 可重看
  - 阵营选择与「已看过序章」存 localStorage（`dr:faction` / `dr:landing-seen`）
- **学院内网 `/console`**：JWT 鉴权后的身份卡页面（档案来自 `dr_user`），支持刷新与退出
- **龙文档案馆 `/archive`**（原首页 `/home` 已退役，`/` 不再指向它）
  - **首页展示**（现位于 `/archive` 与 `/forum`）：学院通报（置顶/紧急分色）、统计面板、两级分类导航（带可见卷宗数）、精选 / 热门 / 最新三个书架、热门检索词
  - **分级分类检索**：关键词 + 一级分类 + 二级分类 + 密级分级 + 4 种排序 + 分页；每个筛选项带实时命中条数（分面统计）
  - **卷宗详情弹窗**：点卡片打开，支持 ESC / 点遮罩关闭，越级访问会被后端拒绝并提示
  - **分级可见**：「密级」= 公开 / 内部 / 机密 / 绝密，按登录身份自动过滤，超出权限的密级筛选项置灰加锁
- **档案室 `/archive`**：五个分卷 —— 龙王卷宗 / 部门人物 / 龙族武器 / 任务执行记录 / **言灵图鉴**
  - 分卷支持 hash 直达：`#tab-kings` / `#tab-people` / `#tab-arms` / `#tab-logs` / `#tab-spirits`（Hash 路由下完整地址形如 `/#/archive#tab-spirits`）
  - 内容按权限点解锁密级，权限进度随账号存后端，换设备登录同一账号自动恢复（也可用备份码跨机搬运）
  - **言灵图鉴原先在首页，现已整体并入此处**：卡片点开即 `/spirit/:key` 言灵档案，数据由后台 `/admin` 维护
- **鉴权**：JWT 令牌（Bearer），拦截器统一校验，401 自动清除本作用域会话并跳回登录页
- **多账号并存**：同一浏览器可同时登录多个账号、互不覆盖（见下节「会话隔离」）
- **路由**：Hash 模式（`createWebHashHistory`），`/console` 由路由守卫保护；构建产物可直接静态托管
- **后台管理 `/admin`**：言灵图鉴增删改查 · **主题管理**（直接发帖、置顶 / 加精 / 上下线 / 删除，不必先等举报）· 举报处理 · 数据概览
- **响应式**：移动端单列自适应，支持 `prefers-reduced-motion`

## 会话隔离（多账号并存）

浏览器的 `localStorage` 是**按 origin 共享**的，不是按标签页、也不是按 URL 路径共享。
同一个 `host:port` 下开两个窗口，默认会把令牌写进同一个键 —— 后登录的账号覆盖先登录的，
两个窗口过一会儿就都变成同一个账号。这就是「串号」。

本项目把会话键改成带作用域的形式：`dr:s:<scope>:token`。

| 作用域来源 | 优先级 | 生命周期 |
|---|---|---|
| URL 参数 `?as=<name>` | 高 | 永久（刷新、重启浏览器后仍在，适合两个账号各存一个书签） |
| 本标签页已确定的作用域（sessionStorage） | 中 | 刷新仍在，关掉标签页即失效 |
| 最近一次用过的作用域 | 低 | 让单账号用户的「记住我」照常生效 |
| `default` | 兜底 | 首次访问 |

三条硬规则：

1. **登录第二个账号时自动换槽**，不覆盖已存在的另一个账号的会话 —— 无需任何操作；
2. **退出 / 令牌过期只清当前作用域**，不会把另一个窗口的账号一起踢下线；
3. 令牌仍按「记住我」决定存 `localStorage`（7 天）还是 `sessionStorage`（仅本次标签页），语义未变。

用法示例（哈希路由下 `?as=` 写在 `#` 之后）：

```
http://192.168.68.1:5177/#/forum?as=student    # 账号 1，存书签
http://192.168.68.1:5177/#/forum?as=teacher    # 账号 2，存书签
```

登录成为「第二个账号」后，登录页会显示当前独立身份槽，跳转地址也会自动带上 `?as=`，方便直接存书签。

回归验证（不需要浏览器，模拟多标签页共享 localStorage）：

```bash
npm run check:session
```

实现位置：`src/utils/sessionScope.js`（作用域名解析与登记）、`src/utils/auth.js`（按作用域读写令牌）。

## 快速开始

```bash
# 1. 后端（..\springBootTest，或直接双击上级目录的「一键启动.bat」）
mvn spring-boot:run          # 或在 IDEA 中直接运行 SpringBootTestApplication

# 2. 前端
npm install
npm run dev      # 开发模式，默认 http://192.168.68.1:5177
npm run build    # 构建到 dist/
npm run preview  # 本地预览构建产物
```

> 前端接口地址：开发环境走 Vite 代理（见 `vite.config.js` 的 `server.proxy`），无需配置；
> 部署时通过 `.env.production` 的 `VITE_API_BASE` 指向后端地址。

## 后端接口（Spring Boot 3 · 统一响应体 `{code, message, data}`）

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| POST | `/api/auth/register/send` | 下发注册验证码（演示环境由系统直接给出，不发邮件） | 否 |
| POST | `/api/auth/register` | 新生登记（需先通过验证码校验），返回签发的学号 | 否 |
| POST | `/api/auth/login` | 身份认证，返回 JWT + 用户信息 | 否 |
| GET | `/api/auth/check-account` | 账号是否已登记（返回行动代号） | 否 |
| POST | `/api/auth/reset/send` | 下发 6 位验证龙文（5 分钟有效，60 秒冷却） | 否 |
| POST | `/api/auth/reset/confirm` | 校验验证龙文并重置密钥 | 否 |
| GET | `/api/auth/dictionaries` | 身份 / 言灵字典（前后端共用 code） | 否 |
| GET | `/api/user/profile` | 我的身份卡 | 是 |
| POST | `/api/user/logout` | 退出登录 | 是 |
| GET | `/api/home/overview` | 首页概览（通报 / 统计 / 分类树 / 精选 · 热门 · 最新 / 热词） | 软鉴权 |
| GET | `/api/search` | 分级分类检索（关键词 + 两级分类 + 密级 + 排序 + 分页 + 分面统计） | 软鉴权 |
| GET | `/api/search/categories` | 两级分类树（带可见卷宗数） | 软鉴权 |
| GET | `/api/search/hot` | 热门检索词 | 软鉴权 |
| GET | `/api/archive/{id}` | 卷宗详情（浏览量 +1） | 软鉴权 |

> **软鉴权** = 接口对所有人开放，但会识别 `Authorization` 头里的身份；
> 不同在校身份能看到不同密级的卷宗（未登录 → 仅公开，新生/在校生 → 内部，执行部 → 机密，教员 → 绝密）。

数据库脚本位于后端项目：

- `sql/dragon_raja.sql` —— 账号库（建库 `dragon_raja`、表 `dr_user` / `dr_reset_code`、三个演示账号）
- `sql/dragon_raja_archive.sql` —— 档案库（5 张表 + 演示卷宗），可重复执行、不含任何 `DROP`

## 目录结构

```
DragonRaja-Vue/
├─ index.html                  # 入口 HTML（含 SVG favicon）
├─ vite.config.js              # base: './'、@ 别名、端口 5177
├─ package.json
└─ src/
   ├─ main.js                  # 应用入口，挂载 router 与全局样式
   ├─ App.vue                  # 布局 + 路由过渡
   ├─ router/index.js          # 路由表 + 守卫（落地层条件入口 / 登录态 / 管理员）
   ├─ styles/theme.css         # 设计令牌 + 全部公共组件样式
   ├─ api/
   │  ├─ http.js               # fetch 封装（统一响应体、401 清理会话、超时）
   │  ├─ auth.js               # 认证相关接口
   │  ├─ home.js               # 首页概览接口（含后端不可用时的兜底数据）
   │  └─ search.js             # 分级分类检索 / 分类树 / 热词 / 卷宗详情
   ├─ data/
   │  └─ portal.js             # 落地层文案 / 双阵营配置 / 图片 srcset 助手
   ├─ components/
   │  ├─ AuthLayout.vue        # 品牌栏 / 舞台 / 页脚外壳
   │  ├─ RuneBackground.vue    # 法阵 + 粒子背景特效（内页用）
   │  ├─ AmbientDust.vue       # 龙文尘埃 Canvas 粒子（落地页用，与上者互斥）
   │  ├─ ProloguePlayer.vue    # 序章逐字打字机 + 进度条
   │  ├─ FactionSwitch.vue     # 双阵营切换（v-model，可键盘操作）
   │  ├─ DragonEmblem.vue      # 学院徽记 SVG
   │  ├─ AuthCard.vue          # 认证卡片（四角封印 + 插槽）
   │  ├─ FormField.vue         # 输入 / 下拉 / 密码字段
   │  ├─ GlowButton.vue        # 鎏金按钮（loading / ghost）
   │  └─ AppIcon.vue           # 线性图标集
   └─ views/
      ├─ LandingView.vue       # 落地页 · 徽记首屏 + 序章（吸收原 longzu 原型）
      ├─ PortalView.vue        # 学院门户 · 双阵营 + 公告 + 登录入口
      ├─ LoginView.vue         # 登录页
      ├─ RegisterView.vue      # 注册页
      ├─ ForgotView.vue        # 密钥重置页（三步向导）
      └─ ConsoleView.vue       # 学院内网 · 我的身份卡

   src/legacy/                 # 已退役视图（不参与构建，供查阅）
      └─ HomeView.vue          # 原首页 · 龙文档案馆（`/` 已改指论坛）
```

## 自定义

- **换主色**：改 `src/styles/theme.css` 顶部 `:root` 里的 `--gold` / `--gold-lt` / `--gold-dp`、`--blood`、`--ice` 即可整体换肤。
- **改校验规则**：登录页 `CODE_RE` 与 `validateAccount()`；注册页 `RULES` 对象。
- **接入真实接口**：把 `LoginView.vue` / `RegisterView.vue` 中 `window.setTimeout(...)` 的模拟逻辑替换为 `fetch` / `axios` 请求即可。

> 本项目为粉丝向演示作品，画面与文案为原创演绎，与原著版权方无关；表单数据不会上传。
