import { request } from './http'

/**
 * 首页展示接口。
 * 后端：springBootTest · HomeController（GET /api/home/overview）
 *
 * 一次请求返回首页所需的全部数据：学院通报 / 统计面板 / 两级分类 /
 * 精选 · 热门 · 最新卷宗 / 热词 / 当前访问者的档案权限。
 * 返回内容按访问者身份的可见密级过滤，不同身份看到的首页不一样。
 */

/** 后端不可用时的兜底数据，保证首页不白屏 */
const FALLBACK = {
  access: { logged: false, codename: '', identityName: '未认证访客', maxLevel: 1, maxLevelLabel: '公开' },
  stats: { archiveCount: 0, categoryCount: 0, topSecretCount: 0, spotlightCount: 0, searchCount: 0 },
  notices: [],
  categories: [],
  spotlight: [],
  hot: [],
  latest: [],
  hotKeywords: ['龙王', '言灵', '尼伯龙根', '血统鉴定', '青铜与火', '村雨']
}

export const homeApi = {
  /**
   * 首页概览。
   * @returns {Promise<object>} 概览数据；后端不可用时返回兜底对象
   */
  async overview() {
    try {
      const data = await request('/api/home/overview', { auth: true })
      return data || FALLBACK
    } catch {
      return FALLBACK
    }
  }
}
