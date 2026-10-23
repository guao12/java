import { clearAdminSession, getAdminToken } from '@/utils/adminAuth'

/**
 * 后台管理系统 API。
 * 与学员端（api/http.js）完全隔离：
 *  - 令牌存独立的 dr:admin:token，互不污染
 *  - 401 时清理后台会话并跳转 /admin/login（不影响学员登录态）
 */

const BASE_URL = (import.meta.env.VITE_API_BASE || '').replace(/\/$/, '')

export class AdminApiError extends Error {
  constructor(message, code) {
    super(message)
    this.name = 'AdminApiError'
    this.code = code
  }
}

/**
 * 后台专用请求。响应约定与学员端一致：code == 0 成功，401 登录失效。
 */
export async function adminRequest(path, { method = 'GET', body, timeout = 15000 } = {}) {
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'

  const token = getAdminToken()
  if (token) headers.Authorization = `Bearer ${token}`

  const controller = new AbortController()
  const timer = window.setTimeout(() => controller.abort(), timeout)

  let response
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal: controller.signal
    })
  } catch (e) {
    const aborted = e.name === 'AbortError'
    throw new AdminApiError(
      aborted ? '请求超时，学院服务器无响应，请稍后重试' : '无法连接学院服务器，请确认后端已启动（默认 127.0.0.1:8082）',
      0
    )
  } finally {
    window.clearTimeout(timer)
  }

  if (response.status === 401) {
    clearAdminSession()
    let message = '后台登录状态已失效，请重新登录'
    try {
      const payload = await response.json()
      if (payload?.message) message = payload.message
    } catch {
      /* 忽略解析失败 */
    }
    throw new AdminApiError(message, 401)
  }

  let payload = null
  try {
    payload = await response.json()
  } catch {
    throw new AdminApiError('服务器返回了无法解析的数据', response.status)
  }

  if (!response.ok) {
    throw new AdminApiError(payload?.message || `请求失败（HTTP ${response.status}）`, response.status)
  }
  if (payload.code !== 0) {
    if (payload.code === 401) clearAdminSession()
    throw new AdminApiError(payload.message || '操作失败', payload.code)
  }

  return payload.data
}

export const adminApi = {
  /**
   * 管理员登录（后台只有登录，没有注册）。
   * @returns {Promise<{token:string, expiresIn:number, admin:{id:number, username:string, nickname:string}}>}
   */
  async login(username, password, remember = false) {
    const data = await adminRequest('/api/admin/auth/login', {
      method: 'POST',
      body: { username, password, remember }
    })
    return data
  },

  /** 当前登录的管理员信息（校验本地令牌是否仍有效） */
  async me() {
    return adminRequest('/api/admin/auth/me')
  },

  /** 言灵列表（含隐藏条目） */
  async listSpirits() {
    return adminRequest('/api/admin/spirit/list')
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
