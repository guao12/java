# WorkBuddy 登录 / 注册页

React 18 + Vite + Tailwind CSS 实现的移动端优先登录/注册页面。

## 特性
- 响应式、移动端优先（桌面端左侧品牌面板 + 右侧表单）
- 组件化拆分，每个组件独立文件（`src/components/`）
- 语义化 HTML + a11y：label/aria、tablist、separator、focus 可见环、prefers-reduced-motion
- 品牌蓝 `#2563eb`，统一在 `tailwind.config.js` 的 `brand` 色阶，便于替换
- 客户端校验：邮箱格式、密码长度、两次密码一致性

## 运行
```bash
npm install
npm run dev      # 本地开发，默认 http://localhost:5173
npm run build    # 生产构建
npm run preview  # 预览构建产物
```

## 目录结构
```
auth-page/
├─ index.html
├─ package.json
├─ vite.config.js
├─ tailwind.config.js
├─ postcss.config.js
└─ src/
   ├─ main.jsx
   ├─ index.css
   ├─ App.jsx
   └─ components/
      ├─ AuthCard.jsx      # 登录/注册主卡片（状态与校验）
      ├─ AuthTabs.jsx      # 登录/注册切换
      ├─ BrandPanel.jsx    # 桌面端左侧品牌区
      ├─ Logo.jsx          # 品牌标识
      ├─ TextField.jsx     # 通用文本输入
      ├─ PasswordField.jsx # 密码输入（显示/隐藏）
      ├─ SubmitButton.jsx  # 主提交按钮
      ├─ Divider.jsx       # 分隔线
      └─ SocialAuth.jsx    # 第三方登录
```

## 接入真实后端
`src/components/AuthCard.jsx` 的 `handleSubmit` 目前用 `alert` 占位，替换为你的登录/注册接口即可。
