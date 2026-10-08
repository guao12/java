/**
 * 极简请求封装，基于原生 fetch，不引入 axios 等额外依赖。
 *
 * 约定：后端所有接口返回 { code, message, data }
 *   code === 200  -> 成功，resolve 出 data
 *   code !== 200  -> 失败，reject 一个带 message 的 Error
 */

const BASE_URL = '/api'

/** 统一错误对象 */
export class ApiError extends Error {
  constructor(message, code, raw) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.raw = raw
  }
}

async function request(url, { method = 'GET', body, params } = {}) {
  let fullUrl = BASE_URL + url

  if (params) {
    const qs = new URLSearchParams(
      Object.entries(params).filter(([, v]) => v !== undefined && v !== null && v !== '')
    ).toString()
    if (qs) fullUrl += (fullUrl.includes('?') ? '&' : '?') + qs
  }

  const options = {
    method,
    headers: { 'Content-Type': 'application/json;charset=UTF-8' },
    // 带上 Cookie，后端的登录态存在 Session 里
    credentials: 'include'
  }
  if (body !== undefined) options.body = JSON.stringify(body)

  let res
  try {
    res = await fetch(fullUrl, options)
  } catch (e) {
    // 网络层失败：后端没启动、端口不对等
    throw new ApiError('无法连接服务器，请确认后端已启动（默认 http://localhost:8081）', -1)
  }

  if (!res.ok) {
    throw new ApiError(`请求失败（HTTP ${res.status}）`, res.status)
  }

  let json
  try {
    json = await res.json()
  } catch (e) {
    throw new ApiError('服务器返回的不是合法 JSON', -2)
  }

  if (json.code !== 200) {
    throw new ApiError(json.message || '请求失败', json.code, json)
  }
  return json.data
}

export const http = {
  get: (url, params) => request(url, { method: 'GET', params }),
  post: (url, body) => request(url, { method: 'POST', body }),
  put: (url, body) => request(url, { method: 'PUT', body }),
  del: (url, params) => request(url, { method: 'DELETE', params })
}
