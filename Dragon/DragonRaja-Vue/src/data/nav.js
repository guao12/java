/**
 * 全院统一顶部导航配置。
 *
 * 说明：
 * - `to`：目标路由；`id`：目标在学院首页时对应的区块锚点（#id）
 * - `match`：这些路径前缀也视为命中该项（人物志 / 言灵谱 / 机构志都归属档案室）
 * - 首页、任务看板、档案室、血统鉴定、档案详情页统一使用这份配置，
 *   由 components/SiteNav.vue 渲染 —— 改这里即可全院同步。
 */
export const SITE_NAV = [
  { label: '首页', to: '/home', id: 'sec-top' },
  { label: '言灵图鉴', to: '/home', id: 'sec-codex' },
  { label: '学院机构', to: '/home', id: 'sec-org' },
  { label: '档案室', to: '/archive', match: ['/archive', '/codex', '/spirit', '/org'] },
  { label: '任务', to: '/missions' },
  { label: '血统鉴定', to: '/appraisal' },
  { label: '后台管理', to: '/admin', match: ['/admin'] }
]

/** 首页路由：品牌点击与锚点跳转的默认落点 */
export const HOME_PATH = '/home'
