import { clearSession, getToken } from '@/utils/auth'

/**
 * 后端地址。
 *  - 开发环境留空：走 Vite 代理（/api → http://127.0.0.1:8082），无跨域问题
 *  - 部署时可设置 VITE_API_BASE=http://host:port 指向后端
 */
const BASE_URL = (import.meta.env.VITE_API_BASE || '').replace(/\/$/, '')

/** 业务异常：message 直接取自后端返回，可直接展示给用户 */
export class ApiError extends Error {
  constructor(message, code) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

export const CODE_UNAUTHORIZED = 401

export async function request(path, { method = 'GET', body, auth = true, timeout = 15000 } = {}) {
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'

  const token = getToken()
  if (auth && token) headers.Authorization = `Bearer ${token}`

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
    throw new ApiError(
      aborted
        ? '请求超时，学院服务器无响应，请稍后重试'
        : '无法连接学院服务器，请确认后端已启动（默认 127.0.0.1:8082）',
      0
    )
  } finally {
    window.clearTimeout(timer)
  }

  // 401：令牌缺失 / 过期，清理本地会话
  if (response.status === 401) {
    clearSession()
    let message = '登录状态已失效，请重新认证'
    try {
      const payload = await response.json()
      if (payload?.message) message = payload.message
    } catch {
      /* 忽略解析失败 */
    }
    throw new ApiError(message, CODE_UNAUTHORIZED)
  }

  let payload = null
  try {
    payload = await response.json()
  } catch {
    throw new ApiError('服务器返回了无法解析的数据', response.status)
  }

  if (!response.ok) {
    throw new ApiError(payload?.message || `请求失败（HTTP ${response.status}）`, response.status)
  }

  // 统一响应体：code = 0 表示成功
  if (payload.code !== 0) {
    if (payload.code === CODE_UNAUTHORIZED) clearSession()
    throw new ApiError(payload.message || '操作失败', payload.code)
  }

  return payload.data
}
