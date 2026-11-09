/**
 * 全站统一顶部导航配置。
 *
 * 定位已从「学院档案系统」迁移为「读者论坛」：
 * 讨论区是第一入口，档案馆降为资料层，任务/血统鉴定退出一级导航。
 *
 * 说明：
 * - `to`：目标路由
 * - `match`：这些路径前缀也视为命中该项（版块页 / 主题页都算「讨论区」）
 * - 一级导航不再放 `id` 锚点项 —— 首页已改为论坛首页，没有长页区块可锚定
 * - 由 components/SiteNav.vue 渲染，改这里即可全站同步
 */
export const SITE_NAV = [
  // 论坛是主站：版块页、主题页、发帖页都归属这一项，避免切换时高亮丢失
  { label: '讨论区', to: '/forum', match: ['/forum', '/board', '/thread'] },
  // 同人区单独给一个入口 —— 这是小说论坛最容易沉淀内容的地方，值得占一级导航
  { label: '同人', to: '/board/fanwork', match: ['/board/fanwork'] },
  // 档案馆：原首页的门面内容降级为资料层（人物志 / 言灵谱 / 机构志都在这里）
  { label: '档案馆', to: '/archive', match: ['/archive', '/codex', '/spirit', '/org'] },
  // 好友：badge = 'unread' 时，导航项右上角显示未读私信数
  // （SiteNav 会轮询 /api/friend/badge，只有登录后才轮询）
  { label: '好友', to: '/friends', match: ['/friends'], badge: 'unread' },
  { label: '我的', to: '/console' }
]

/**
 * 后台管理系统入口。
 * 不混在 SITE_NAV 里 —— 它渲染成导航栏右侧的独立描边按钮（带盾牌图标），
 * 与前台菜单区分开。点进去由路由守卫决定落点：
 * 已登录管理员 → /admin；未登录 → /admin/login（登录页只有登录，没有注册）。
 */
export const ADMIN_ENTRY = { label: '后台管理', to: '/admin' }

/** 首页路由：品牌点击与默认落点都指向论坛首页 */
export const HOME_PATH = '/forum'
