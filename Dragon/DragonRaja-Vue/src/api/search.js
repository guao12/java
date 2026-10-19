import { request } from './http'

/**
 * 档案检索接口（分级 + 分类）。
 * 后端：springBootTest · SearchController
 *
 *  GET /api/search             分级分类检索（关键词 + 两级分类 + 密级 + 排序 + 分页）
 *  GET /api/search/categories  两级分类树（带可见档案数）
 *  GET /api/search/hot         热门检索词
 *  GET /api/archive/{id}       卷宗详情（浏览量 +1；越级访问返回业务错误）
 *
 * 「分级」= 密级（公开 / 内部 / 机密 / 绝密），后端按访问者身份自动过滤，
 * 前端只需把用户的选择原样传给后端即可。
 */

/** 后端不可用时的兜底热词 */
const FALLBACK_HOT = ['龙王', '言灵', '尼伯龙根', '血统鉴定', '青铜与火', '村雨']

/** 把筛选对象拼成查询串，空值不传 */
function toQuery(params = {}) {
  const qs = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => {
    if (v === null || v === undefined || v === '' || v === 'all') return
    qs.append(k, v)
  })
  const s = qs.toString()
  return s ? `?${s}` : ''
}

export const searchApi = {
  /**
   * 分级分类检索。
   *
   * @param {object} params
   * @param {string} [params.keyword]      关键词
   * @param {number|string} [params.topCategory] 一级分类 ID
   * @param {number|string} [params.category]    二级分类 ID
   * @param {number|string} [params.level]       密级 1-4
   * @param {string} [params.sort]         heat(默认) / new / view / level
   * @param {number} [params.page]         页码，从 1 开始
   * @param {number} [params.size]         每页条数
   * @returns {Promise<{page:object, topCategoryFacets:Array, levelFacets:Array, echo:object, total:number}>}
   */
  search(params) {
    return request(`/api/search${toQuery(params)}`, { auth: true })
  },

  /** 两级分类树（含各分类下可见档案数） */
  async categories() {
    try {
      return await request('/api/search/categories', { auth: true })
    } catch {
      return []
    }
  },

  /** 热门检索词 */
  async hot() {
    try {
      const data = await request('/api/search/hot', { auth: true })
      return Array.isArray(data) && data.length ? data : FALLBACK_HOT
    } catch {
      return FALLBACK_HOT
    }
  },

  /** 卷宗详情 */
  detail(id) {
    return request(`/api/archive/${id}`, { auth: true })
  },

  /** 兜底热词（组件初始化前用） */
  hotKeywords: FALLBACK_HOT
}
