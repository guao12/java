# 卡塞尔学院 · 龙族身份认证系统（Vue 3）

龙族（Dragon Raja）同人风格的登录 / 注册页面，Vue 3 + Vite 单页应用。
已对接 **Spring Boot 3 + MyBatis-Plus + MySQL 8** 后端（`D:\workSpace\springBootTest`），登录 / 注册 / 密钥重置均为真实接口。

## 端到端流程

```
浏览器 → Vite dev(5177) --代理 /api--> Spring Boot(8081) --MyBatis-Plus--> MySQL(dragon_raja)
```

1. 启动后端（默认 dev profile，端口 8081，需先执行后端项目的 `sql/dragon_raja.sql`）
2. `npm run dev` 启动前端，登录页底部会显示「后端通道 · 已连接（127.0.0.1:8081）」
3. 演示账号：`CC20260001`（或 `luffy.cassell@cassell.edu`），密钥 `Dragon@2026`

## 特性

- **龙族主题视觉**：暗夜深渊底色 + 鎏金描边 + 玻璃拟态认证卡片
- **动态背景**：Canvas 悬浮龙文尘埃 / 上升流火粒子 + SVG 旋转龙文法阵 + 巨型「龍」水印
- **登录页**：学号 / 邮箱校验、密钥显隐、记住身份卡、演示身份卡一键填充、后端连通状态指示
- **注册页**：双列布局、身份与言灵下拉（下拉项来自后端字典接口）、密码强度四档评估、协议勾选校验、提交后展示后端签发的学院编号并自动预填到登录页
- **密钥重置页 `/forgot`**：三步向导（验证身份 → 重置密钥 → 完成），验证龙文由后端下发（5 分钟有效 / 60 秒冷却）、重置后旧密钥失效
- **学院内网 `/console`**：JWT 鉴权后的身份卡页面（档案来自 `dr_user`），支持刷新与退出
- **鉴权**：JWT 令牌（Bearer），拦截器统一校验，401 自动清除本地会话并跳回登录页
- **路由**：Hash 模式（`createWebHashHistory`），`/console` 由路由守卫保护；构建产物可直接静态托管
- **响应式**：移动端单列自适应，支持 `prefers-reduced-motion`

## 快速开始

```bash
# 1. 后端（D:\workSpace\springBootTest）
mvn spring-boot:run          # 或在 IDEA 中直接运行 SpringBootTestApplication

# 2. 前端
npm install
npm run dev      # 开发模式，默认 http://127.0.0.1:5177
npm run build    # 构建到 dist/
npm run preview  # 本地预览构建产物
```

> 前端接口地址：开发环境走 Vite 代理（见 `vite.config.js` 的 `server.proxy`），无需配置；
> 部署时通过 `.env.production` 的 `VITE_API_BASE` 指向后端地址。

## 后端接口（Spring Boot 3 · 统一响应体 `{code, message, data}`）

| 方法 | 路径 | 说明 | 鉴权 |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | 新生登记，返回签发的学号 | 否 |
| POST | `/api/auth/login` | 身份认证，返回 JWT + 用户信息 | 否 |
| GET | `/api/auth/check-account` | 账号是否已登记（脱敏邮箱） | 否 |
| POST | `/api/auth/reset/send` | 下发 6 位验证龙文（5 分钟有效，60 秒冷却） | 否 |
| POST | `/api/auth/reset/confirm` | 校验验证龙文并重置密钥 | 否 |
| GET | `/api/auth/dictionaries` | 身份 / 言灵字典（前后端共用 code） | 否 |
| GET | `/api/user/profile` | 我的身份卡 | 是 |
| POST | `/api/user/logout` | 退出登录 | 是 |

数据库脚本位于后端项目 `sql/dragon_raja.sql`（建库 `dragon_raja`、表 `dr_user` / `dr_reset_code`、三个演示账号）。

## 目录结构

```
DragonRaja-Vue/
├─ index.html                  # 入口 HTML（含 SVG favicon）
├─ vite.config.js              # base: './'、@ 别名、端口 5177
├─ package.json
└─ src/
   ├─ main.js                  # 应用入口，挂载 router 与全局样式
   ├─ App.vue                  # 布局 + 路由过渡
   ├─ router/index.js          # /login、/register 路由与页面标题
   ├─ styles/theme.css         # 设计令牌 + 全部公共组件样式
   ├─ components/
   │  ├─ AuthLayout.vue        # 品牌栏 / 舞台 / 页脚外壳
   │  ├─ RuneBackground.vue    # 法阵 + 粒子背景特效
   │  ├─ DragonEmblem.vue      # 学院徽记 SVG
   │  ├─ AuthCard.vue          # 认证卡片（四角封印 + 插槽）
   │  ├─ FormField.vue         # 输入 / 下拉 / 密码字段
   │  ├─ GlowButton.vue        # 鎏金按钮（loading / ghost）
   │  └─ AppIcon.vue           # 线性图标集
   └─ views/
      ├─ LoginView.vue         # 登录页
      ├─ RegisterView.vue      # 注册页
      └─ ForgotView.vue        # 密钥重置页（三步向导）
```

## 自定义

- **换主色**：改 `src/styles/theme.css` 顶部 `:root` 里的 `--gold` / `--gold-lt` / `--gold-dp`、`--blood`、`--ice` 即可整体换肤。
- **改校验规则**：登录页 `EMAIL_RE`、`CODE_RE` 与 `validateAccount()`；注册页 `RULES` 对象。
- **接入真实接口**：把 `LoginView.vue` / `RegisterView.vue` 中 `window.setTimeout(...)` 的模拟逻辑替换为 `fetch` / `axios` 请求即可。

> 本项目为粉丝向演示作品，画面与文案为原创演绎，与原著版权方无关；表单数据不会上传。
