# 文档中心

> 2026-11-16 整理：原先散落在项目根目录与前端目录的文档统一收进 `docs/`，并修正了所有引用路径。

## 导航

| 文档 | 内容 | 什么时候看 |
| --- | --- | --- |
| [architecture/整合方案.md](architecture/整合方案.md) | 原 `longzu` 单页原型并入本工程的方案、冲突处置与验收清单 | 想知道"为什么现在长这样" |
| [design/登录页改版方案.md](design/登录页改版方案.md) | 登录页 v1 改版的视觉与交互依据（对照原型 CSS/DOM 行号） | 要再改登录页时 |
| [guides/AI项目生成步骤.txt](guides/AI项目生成步骤.txt) | 从"生成一个 HTML 网页"到"变成 Vue 工程"的提示词过程记录 | 想复刻这套生成流程时 |
| [frontend/AI-MEMO.md](frontend/AI-MEMO.md) | 前端唯一的项目上下文备忘：踩过的坑、环境现状、快速启动 | **接手前必读** |
| [backend/HELP.md](backend/HELP.md) | Spring Initializr 生成的原始帮助页（参考用，非项目文档） | 基本不需要 |
| [qa/screenshots/](qa/screenshots/) | 7 张验收截图（登录卡、论坛流、好友流、后台主题、任务日志） | 想对照当前 UI 是否走样 |

## 目录结构

```
docs/
├── README.md                       ← 本文件
├── architecture/
│   └── 整合方案.md
├── design/
│   └── 登录页改版方案.md
├── guides/
│   └── AI项目生成步骤.txt
├── frontend/
│   └── AI-MEMO.md
├── backend/
│   └── HELP.md
└── qa/
    └── screenshots/                ← 7 张验收截图
```

## 刻意没有搬走的两份文档

- `springBootTest/README.md`（后端说明）
- `DragonRaja-Vue/README.md`（前端功能清单与接口表）

模块 README 留在模块根目录是通行约定，IDE、GitHub 与 `一键启动.bat` 都默认从模块根找它；
把它们挪进 `docs/` 反而会让接手的人在模块里找不到入口。根 `README.md` 已提供跳转链接。

## 迁移记录（2026-11-16）

| 原路径 | 现路径 |
| --- | --- |
| `整合方案.md` | `docs/architecture/整合方案.md` |
| `登录页改版方案.md` | `docs/design/登录页改版方案.md` |
| `AI项目生成步骤.txt` | `docs/guides/AI项目生成步骤.txt` |
| `DragonRaja-Vue/AI-MEMO.md` | `docs/frontend/AI-MEMO.md` |
| `springBootTest/HELP.md` | `docs/backend/HELP.md` |

同步修正的引用：根 `README.md`、「子文档」区、`longzu/DEPRECATED.md` 的回滚指引、`整合方案.md` 内的自引用与 `AI-MEMO.md` / `AI项目生成步骤.txt` 的路径。

## 附：CDP 探针脚本

`DragonRaja-Vue/scripts/dev-probes/` 下是 6 个一次性 CDP 验证脚本，原先混在 Chrome 的
`.cdp-prof/` 配置目录里（该目录已于同日删除），现单独存放：

| 脚本 | 校验内容 |
| --- | --- |
| `login-check.mjs` | 登录页身份卡不再渲染「血统等级」块，并截图 |
| `admin-check.mjs` | 后台「主题管理」面板渲染与截图 |
| `flow-check2.mjs` | 登录态 → 讨论区 → 好友 → 返回讨论区，确认不再出现 500 提示 |
| `check-logs.mjs` | `logDetail.js` 对全部 22 条 ARCHIVE_MISSION_LOGS 的覆盖与篇幅 |
| `verify3.mjs` | 通用页面校验 |
| `vite-check.mjs` | Vite 侧可用性检查 |

这些脚本依赖本地已启动的前后端与 Chrome CDP 端口，属于开发期临时工具，不参与构建。
