/**
 * 会话作用域（身份槽）
 * ---------------------------------------------------------------
 * 解决的问题：浏览器的 localStorage 是「按 origin 共享」的，不是按标签页、
 * 也不是按 URL 路径共享。同一个 host:port 下开两个窗口，写的是同一份
 * dr:token —— 后登录的账号会覆盖先登录的，于是两个窗口过一会儿都变成
 * 同一个账号。
 *
 * 做法：给每个「身份」一个作用域名，会话键写成 dr:s:<scope>:token。
 * 作用域名的来源，优先级从高到低：
 *   1. URL 上的 ?as=<name>（显式指定，刷新/重启浏览器都还在，适合长期双开）
 *      哈希路由下的 ?as= 写在 # 之后：http://host:5177/#/forum?as=student
 *   2. 本标签页已确定的作用域（sessionStorage，刷新仍在、关掉标签页即失效）
 *   3. 最近一次用过的作用域（让单账号用户的「记住我」不退化）
 *   4. 'default'
 *
 * 注意：sessionStorage 天然按标签页隔离，这正是我们要的粒度；localStorage
 * 只用来存放各作用域的持久会话，键名互不相交。
 */

const TAB_KEY = 'dr:tab-scope'
const INDEX_KEY = 'dr:scopes'
const INDEX_LIMIT = 8
const DEFAULT_SCOPE = 'default'
const NAME_RE = /^[a-z0-9_-]{1,24}$/

/** 内存缓存；真实浏览器里每个标签页各有一份，天然互不影响 */
let cached = ''

function safeStorage(kind) {
  try {
    const store = typeof window !== 'undefined' ? window[kind] : undefined
    if (store) {
      // 隐私模式下 getItem 可能抛异常，探测一次
      store.getItem(TAB_KEY)
      return store
    }
  } catch {
    /* ignore */
  }
  return null
}

const ls = () => safeStorage('localStorage')
const ss = () => safeStorage('sessionStorage')

function sanitize(value) {
  if (typeof value !== 'string') return ''
  const v = value.trim().toLowerCase()
  return NAME_RE.test(v) ? v : ''
}

/** 从地址栏读显式作用域：?as=xxx，兼容 ?as=xxx#/path 与 #/path?as=xxx 两种写法 */
export function readUrlScope() {
  if (typeof window === 'undefined') return ''
  const candidates = []
  const search = window.location?.search
  if (search && search.length > 1) candidates.push(search.slice(1))
  const hash = window.location?.hash || ''
  const q = hash.indexOf('?')
  if (q >= 0) candidates.push(hash.slice(q + 1))

  for (const raw of candidates) {
    try {
      const name = sanitize(new URLSearchParams(raw).get('as'))
      if (name) return name
    } catch {
      /* 非法查询串，忽略 */
    }
  }
  return ''
}

function readTab() {
  return sanitize(ss()?.getItem(TAB_KEY))
}

function writeTab(name) {
  try {
    ss()?.setItem(TAB_KEY, name)
  } catch {
    /* 存不了也不影响本次会话 */
  }
}

/** 已登记的作用域，按最近使用倒序 */
export function listScopes() {
  try {
    const raw = JSON.parse(ls()?.getItem(INDEX_KEY) || '[]')
    if (!Array.isArray(raw)) return []
    return raw
      .filter((item) => item && typeof item.name === 'string' && NAME_RE.test(item.name))
      .map((item) => ({ name: item.name, at: Number(item.at) || 0 }))
      .sort((a, b) => b.at - a.at)
  } catch {
    return []
  }
}

export function registerScope(name) {
  const key = sanitize(name)
  if (!key) return
  const rest = listScopes().filter((item) => item.name !== key)
  const next = [{ name: key, at: Date.now() }, ...rest].slice(0, INDEX_LIMIT)
  try {
    ls()?.setItem(INDEX_KEY, JSON.stringify(next))
  } catch {
    /* ignore */
  }
}

function forgetScope(name) {
  const next = listScopes().filter((item) => item.name !== name)
  try {
    ls()?.setItem(INDEX_KEY, JSON.stringify(next))
  } catch {
    /* ignore */
  }
}

function newScopeName() {
  const stamp = Date.now().toString(36).slice(-4)
  const rand = Math.random().toString(36).slice(2, 5).padEnd(3, '0')
  return `s${stamp}${rand}`
}

function pickInitial() {
  // 单账号用户：沿用最近用过的槽，「记住我」照常生效
  const latest = listScopes()[0]
  return latest ? latest.name : DEFAULT_SCOPE
}

/** 当前作用域名（幂等，首次调用会确定并写入本标签页） */
export function currentScope() {
  const fromUrl = readUrlScope()
  if (fromUrl && fromUrl !== cached) {
    cached = fromUrl
    writeTab(fromUrl)
    registerScope(fromUrl)
  }
  if (!cached) {
    cached = readTab() || pickInitial()
    writeTab(cached)
    registerScope(cached)
  }
  return cached
}

/** 显式切换作用域（URL 带 ?as= 时自动走这里） */
export function setScope(name) {
  const key = sanitize(name)
  if (!key) return currentScope()
  cached = key
  writeTab(key)
  registerScope(key)
  return key
}

/**
 * 开一个全新的空作用域。
 * 第二个账号登录时调用——不覆盖已有账号的会话，各占一个槽。
 */
export function forkScope() {
  const name = newScopeName()
  cached = name
  writeTab(name)
  registerScope(name)
  return name
}

/** 注销某个作用域的登记信息（清空后回收，避免索引无限增长） */
export function dropScope(name) {
  const key = sanitize(name)
  if (!key) return
  forgetScope(key)
}

/** 仅供测试/调试：丢弃内存缓存，让下次调用重新从存储推导 */
export function resetScopeCache() {
  cached = ''
}
