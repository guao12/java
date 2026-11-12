# 本目录已归档（DEPRECATED）

这个 `longzu` 单页原型（`index.html` + 7 张 PNG）的内容已经并入统一工程：

**`D:\workSpace\java\Dragon\DragonRaja-Vue`**

## 去向对照

| 原文件 / 能力 | 现在的位置 |
| --- | --- |
| Splash 徽记首屏 | `src/views/LandingView.vue` |
| 序章打字机（STORY 五段） | `src/components/ProloguePlayer.vue` + `src/data/portal.js` |
| 门户页（双阵营 / 公告 / 登录 CTA） | `src/views/PortalView.vue` |
| nav-pills 阵营切换 | `src/components/FactionSwitch.vue` |
| Canvas 尘埃粒子 | `src/components/AmbientDust.vue` |
| 登录页 | 放弃原型版本，改用 `src/views/LoginView.vue`（真实 JWT 鉴权） |
| 音乐 FAB | 删除（原型只切图标，没有音频源） |
| `assets/*.png` | 4 张在用图已转 WebP 至 `DragonRaja-Vue/public/img/landing/`；3 张未被引用的已不随工程分发 |
| 全局 `overflow:hidden` 等样式 | 不迁移（会锁死 SPA 内其它页面的滚动） |

## 为什么保留

按整合方案约定保留 30 天作为回滚参照，之后可安全删除。若要回滚，删掉 `DragonRaja-Vue` 里新增的落地层文件与路由即可，业务层未做任何改动。

迁移方案与验收清单见：`D:\workSpace\java\Dragon\整合方案.md`
