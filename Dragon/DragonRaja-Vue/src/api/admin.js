import { createRequest, toQuery } from '@/api/request'
import { clearAdminSession, getAdminToken } from '@/utils/adminAuth'

/**
 * 后台管理系统 API。
 * 与学员端（api/http.js）完全隔离：
 *  - 令牌存独立的 dr:admin:token，互不污染
 *  - 401 时清理后台会话并跳转 /admin/login（不影响学员登录态）
 * 请求流程本身与学员端共用 api/request.js 的 createRequest。
 */

export class AdminApiError extends Error {
  constructor(message, code) {
    super(message)
    this.name = 'AdminApiError'
    this.code = code
  }
}

/**
 * 后台专用请求。响应约定与学员端一致：code == 0 成功，401 登录失效。
 * GET 默认走 3 秒内存缓存 + 并发去重，写操作会自动失效 /api/admin/spirit 下的缓存。
 */
export const adminRequest = createRequest({
  getToken: getAdminToken,
  onUnauthorized: clearAdminSession,
  ErrorClass: AdminApiError,
  unauthorizedMessage: '后台登录状态已失效，请重新登录'
})

/**
 * 统一分页返回结构，兼容后端两种返回：
 *  - 已支持分页：{ list, total } / { records, total } → { list, total, paged: true }
 *  - 仍是全量数组：[...]                              → 前端自行切片分页
 */
function normalizePage(raw, page, size) {
  if (Array.isArray(raw)) {
    const total = raw.length
    const start = (page - 1) * size
    return { list: raw.slice(start, start + size), total, paged: false }
  }
  const list = Array.isArray(raw?.list)
    ? raw.list
    : Array.isArray(raw?.records)
      ? raw.records
      : Array.isArray(raw?.rows)
        ? raw.rows
        : []
  const total = Number(raw?.total ?? raw?.totalElements ?? list.length) || 0
  return { list, total, paged: true }
}

export const adminApi = {
  /**
   * 管理员登录（后台只有登录，没有注册）。
   * @returns {Promise<{token:string, expiresIn:number, admin:{id:number, username:string, nickname:string}}>}
   */
  async login(username, password, remember = false) {
    // 清掉上一位管理员可能留下的读缓存
    adminRequest.clearCache()
    const data = await adminRequest('/api/admin/auth/login', {
      method: 'POST',
      body: { username, password, remember },
      invalidate: false
    })
    return data
  },

  /** 当前登录的管理员信息（校验本地令牌是否仍有效） */
  async me() {
    return adminRequest('/api/admin/auth/me')
  },

  /**
   * 言灵列表（含隐藏条目）。
   * 有筛选词 / 翻页时把参数交给后端；后端若还没实现分页，返回数组也能正确切片。
   *
   * @param {object} [query]
   * @param {string} [query.keyword] 按 key / 名称 / 元素 / 密级过滤
   * @param {number} [query.page] 页码，从 1 开始
   * @param {number} [query.size] 每页条数
   * @param {boolean}[query.fresh] 跳过缓存（手动刷新用）
   * @returns {Promise<{list:Array, total:number, paged:boolean}>}
   */
  async listSpirits(query = {}) {
    const page = Math.max(1, Number(query.page) || 1)
    const size = Math.max(1, Number(query.size) || 20)
    const keyword = (query.keyword || '').trim()

    // 无筛选且第一页时不额外带参，与旧后端完全兼容
    const params = {}
    if (keyword) params.keyword = keyword
    if (page > 1) params.page = page
    if (size !== 20) params.size = size

    const data = await adminRequest(`/api/admin/spirit/list${toQuery(params)}`, {
      fresh: !!query.fresh,
      // 带筛选词的缓存更短，避免改完数据看不到最新结果
      cacheTtl: keyword ? 1200 : 3000
    })
    return normalizePage(data, page, size)
  },

  /** 新增言灵 */
  async createSpirit(form) {
    return adminRequest('/api/admin/spirit', { method: 'POST', body: form })
  },

  /** 编辑言灵 */
  async updateSpirit(id, form) {
    return adminRequest(`/api/admin/spirit/${id}`, { method: 'PUT', body: form })
  },

  /** 删除言灵 */
  async deleteSpirit(id) {
    return adminRequest(`/api/admin/spirit/${id}`, { method: 'DELETE' })
  }
}
