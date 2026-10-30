/**
 * 请求层公共工厂。
 *
 * 学员端（api/http.js）与后台（api/admin.js）的请求逻辑完全一致：
 * 拼 header → 带令牌 → fetch + 超时中断 → 401 清会话 → 解析 JSON → 校验 code。
 * 两端唯一的差异只有「令牌从哪来」「401 清哪个会话」「抛哪种错误类」，
 * 因此这里把流程抽成一份，由调用方传入这三个差异点。
 *
 * 性能相关能力（都放在这一层，学员端 / 后台自动共享）：
 *  - inflight 去重：同一 GET 并发调用只发一条请求（多个组件同时取同一份数据）
 *  - GET 轻缓存：cacheTtl 内直接返回内存里的结果，切菜单 / 反复进页零等待
 *  - 写操作自动失效：POST/PUT/DELETE 成功后按 invalidation 前缀清掉受影响的 GET 缓存
 *  - 断网不重试、超时不重试、5xx/网络抖动自动重试一次
 *  - 查询串里递归剔除 undefined / 空串，避免把 "undefined" 发给后端
 */

/**
 * 后端地址。
 *  - 开发环境留空：走 Vite 代理（/api → http://127.0.0.1:8082），无跨域问题
 *  - 部署时可设置 VITE_API_BASE=http://host:port 指向后端
 */
export const BASE_URL = (import.meta.env.VITE_API_BASE || '').replace(/\/$/, '')

export const CODE_UNAUTHORIZED = 401

/** GET 默认缓存时长：3 秒内重复进入同一接口直接命中内存 */
const DEFAULT_GET_TTL = 3000

/**
 * 把参数对象拼成查询串，并剔除空值。
 * @returns {string} 形如 "?a=1&b=2"；无有效参数时返回空串
 */
export function toQuery(params) {
  if (!params) return ''
  const search = new URLSearchParams()
  for (const [k, v] of Object.entries(params)) {
    if (v === undefined || v === null || v === '') continue
    search.append(k, String(v))
  }
  const qs = search.toString()
  return qs ? `?${qs}` : ''
}

/**
 * 创建一个请求函数。
 *
 * @param {object} options
 * @param {() => string|null} options.getToken   取当前会话令牌
 * @param {() => void} options.onUnauthorized     401 时清理对应会话
 * @param {new (message: string, code: number) => Error} options.ErrorClass 该端使用的错误类
 * @param {string} options.unauthorizedMessage    401 且后端未给 message 时的兜底文案
 * @returns {(path: string, opts?: object) => Promise<any>}
 */
export function createRequest({
  getToken,
  onUnauthorized,
  ErrorClass,
  unauthorizedMessage
}) {
  /** 进行中的请求：同 key 并发调用共享同一个 Promise */
  const inflight = new Map()
  /** GET 结果缓存：key → { at, data } */
  const cache = new Map()

  /** 请求 key：令牌只取长度参与，避免把敏感串留在内存表里 */
  function keyOf(method, path, token) {
    return `${method} ${path} #${token ? token.length : 0}`
  }

  /**
   * 失效缓存。
   * @param {string} prefix 只清 path 里包含该子串的条目；为空则全清
   */
  function invalidate(prefix = '') {
    const drop = (map) => {
      for (const key of [...map.keys()]) {
        if (!prefix || key.includes(prefix)) map.delete(key)
      }
    }
    drop(cache)
    drop(inflight)
  }

  /** 发一条真实请求（不含缓存 / 去重 / 重试） */
  async function send(path, { method, body, auth, timeout }) {
    const headers = { Accept: 'application/json' }
    if (body !== undefined) headers['Content-Type'] = 'application/json'

    const token = auth ? getToken() : null
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
      const err = new ErrorClass(
        aborted
          ? '请求超时，学院服务器无响应，请稍后重试'
          : '无法连接学院服务器，请确认后端已启动（默认 127.0.0.1:8082）',
        0
      )
      err.retryable = !aborted
      throw err
    } finally {
      window.clearTimeout(timer)
    }

    // HTTP 401：令牌缺失 / 过期，清理本地会话
    if (response.status === CODE_UNAUTHORIZED) {
      onUnauthorized()
      let message = unauthorizedMessage
      try {
        const payload = await response.json()
        if (payload?.message) message = payload.message
      } catch {
        /* 忽略解析失败 */
      }
      throw new ErrorClass(message, CODE_UNAUTHORIZED)
    }

    let payload = null
    try {
      payload = await response.json()
    } catch {
      const err = new ErrorClass('服务器返回了无法解析的数据', response.status)
      err.retryable = response.status >= 500
      throw err
    }

    if (!response.ok) {
      const err = new ErrorClass(
        payload?.message || `请求失败（HTTP ${response.status}）`,
        response.status
      )
      // 5xx 多为瞬时故障，值得再试一次
      err.retryable = response.status >= 500
      throw err
    }

    // 统一响应体：code = 0 表示成功
    if (payload.code !== 0) {
      if (payload.code === CODE_UNAUTHORIZED) onUnauthorized()
      throw new ErrorClass(payload.message || '操作失败', payload.code)
    }

    return payload.data
  }

  /** 带一次自动重试的请求 */
  async function sendWithRetry(path, opts) {
    try {
      return await send(path, opts)
    } catch (e) {
      if (!e.retryable || e.code === CODE_UNAUTHORIZED) throw e
      // 重试前稍作停顿，给后端一点恢复时间
      await new Promise((r) => window.setTimeout(r, 260))
      return send(path, opts)
    }
  }

  /**
   * @param {string} path 接口路径
   * @param {object} [opts]
   * @param {string} [opts.method] GET | POST | PUT | DELETE
   * @param {any}    [opts.body] 请求体（自动 JSON 序列化）
   * @param {boolean}[opts.auth] 是否带令牌，默认 true
   * @param {number} [opts.timeout] 超时毫秒，默认 15s
   * @param {number} [opts.cacheTtl] 仅 GET 有效：结果缓存毫秒，0 = 不缓存
   * @param {boolean}[opts.fresh] 仅 GET 有效：跳过缓存强制请求（手动刷新按钮用）
   * @param {string|false} [opts.invalidate] 写操作成功后要失效的缓存前缀，默认按资源路径推导
   */
  async function request(
    path,
    {
      method = 'GET',
      body,
      auth = true,
      timeout = 15000,
      cacheTtl = DEFAULT_GET_TTL,
      fresh = false,
      invalidate: invalidatePrefix
    } = {}
  ) {
    const upper = method.toUpperCase()
    const isRead = upper === 'GET'
    const key = keyOf(upper, path, auth ? getToken() : null)

    if (isRead) {
      if (!fresh && cacheTtl > 0) {
        const hit = cache.get(key)
        if (hit && Date.now() - hit.at < cacheTtl) return hit.data
      }
      const running = inflight.get(key)
      if (running) return running
    }

    const task = (async () => {
      const data = await sendWithRetry(path, { method: upper, body, auth, timeout })

      if (isRead) {
        if (cacheTtl > 0) cache.set(key, { at: Date.now(), data })
        return data
      }

      // 写操作：清掉受影响的读缓存
      if (invalidatePrefix !== false) {
        const prefix = invalidatePrefix || path.split('?')[0].replace(/\/\d+$/, '')
        invalidate(prefix)
      }
      return data
    })()

    inflight.set(key, task)
    try {
      return await task
    } finally {
      // 只清理自己，避免把 later 的同类请求误删
      if (inflight.get(key) === task) inflight.delete(key)
    }
  }

  request.invalidate = invalidate
  request.clearCache = () => invalidate('')
  return request
}
