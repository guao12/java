/**
 * 会话管理：JWT 令牌 + 用户信息缓存。
 *
 * ⚠ 关键约定（2026-11 修复串号问题后）：
 * localStorage 是按 origin 共享的，同一个 host:port 下所有标签页读写同一份键。
 * 所以会话键必须带作用域前缀 —— dr:s:<scope>:token，作用域名由 utils/sessionScope
 * 解析（?as= 或标签页级）。
 *
 * 由此带来三条硬规则：
 *   1. saveSession 绝不覆盖「另一个账号」在本作用域的会话，而是自动换到新作用域；
 *   2. clearSession 只清当前作用域，退出一个账号不会把另一个账号踢下线；
 *   3. 读取时先看本作用域的 sessionStorage（标签页内），再看本作用域的 localStorage。
 */

import { currentScope, dropScope, forkScope } from '@/utils/sessionScope'

const TOKEN_FIELD = 'token'
const USER_FIELD = 'user'

/** 旧版全局键，仅用于一次性迁移 */
const LEGACY_TOKEN = 'dr:token'
const LEGACY_USER = 'dr:user'

const prefix = (scope) => `dr:s:${scope}:`
const tokenKey = (scope) => `${prefix(scope)}${TOKEN_FIELD}`
const userKey = (scope) => `${prefix(scope)}${USER_FIELD}`

function safeStorage(kind) {
  try {
    const store = typeof window !== 'undefined' ? window[kind] : undefined
    if (store) {
      store.getItem(LEGACY_TOKEN)
      return store
    }
  } catch {
    /* 隐私模式等场景降级为无存储 */
  }
  return null
}

const ls = () => safeStorage('localStorage')
const ss = () => safeStorage('sessionStorage')

/** 账号身份指纹：优先 id，退化到学号/代号，用于判定「是不是同一个账号」 */
function identityOf(user) {
  if (!user || typeof user !== 'object') return ''
  const raw = user.id ?? user.userId ?? user.account ?? user.codename
  return raw == null ? '' : String(raw)
}

/* ---------------------------------------------------------------
 * 旧数据迁移：老版本把令牌写在全局键上，升级后第一次访问时搬进
 * 当前作用域，避免用户被强制重登。
 * --------------------------------------------------------------- */
let migrated = false
function migrateLegacy() {
  if (migrated) return
  migrated = true
  const scope = currentScope()
  for (const store of [ls(), ss()]) {
    if (!store) continue
    const oldToken = store.getItem(LEGACY_TOKEN)
    if (oldToken == null) continue
    // 当前作用域已经有会话时不要覆盖，直接把旧键清掉
    if (store.getItem(tokenKey(scope)) == null) {
      store.setItem(tokenKey(scope), oldToken)
      const oldUser = store.getItem(LEGACY_USER)
      if (oldUser != null) store.setItem(userKey(scope), oldUser)
    }
    store.removeItem(LEGACY_TOKEN)
    store.removeItem(LEGACY_USER)
  }
}

/** 读取指定作用域的会话；标签页内的 sessionStorage 优先于共享的 localStorage */
function readScope(scope) {
  const tabStore = ss()
  const localStore = ls()
  const tabToken = tabStore?.getItem(tokenKey(scope)) ?? null
  if (tabToken != null) {
    return { token: tabToken, rawUser: tabStore.getItem(userKey(scope)) }
  }
  const localToken = localStore?.getItem(tokenKey(scope)) ?? null
  return {
    token: localToken,
    rawUser: localToken == null ? null : localStore.getItem(userKey(scope))
  }
}

function writeScope(scope, token, rawUser, remember) {
  const target = remember ? ls() : ss()
  const other = remember ? ss() : ls()
  // 只清本作用域：同一个槽里既不该同时存在两套令牌
  other?.removeItem(tokenKey(scope))
  other?.removeItem(userKey(scope))
  target?.setItem(tokenKey(scope), token)
  target?.setItem(userKey(scope), rawUser)
}

export function getToken() {
  migrateLegacy()
  return readScope(currentScope()).token
}

export function getUser() {
  migrateLegacy()
  const { rawUser } = readScope(currentScope())
  if (!rawUser) return null
  try {
    return JSON.parse(rawUser)
  } catch {
    return null
  }
}

export function isLoggedIn() {
  return !!getToken()
}

/** 当前身份槽的名字（登录页用于提示「本窗口是独立身份」） */
export function currentScopeName() {
  return currentScope()
}

/**
 * 保存会话；remember = true 时写入 localStorage，浏览器关闭后仍保留。
 *
 * 如果当前作用域里已经躺着「另一个账号」的会话，不会覆盖它 ——
 * 而是自动开一个新作用域。这是两个账号能各占一个窗口的关键。
 *
 * @returns 实际写入的作用域名
 */
export function saveSession({ token, user }, remember = true) {
  migrateLegacy()
  let scope = currentScope()
  const prev = readScope(scope)
  const prevId = identityOf(safeParse(prev.rawUser))
  const nextId = identityOf(user)

  if (prev.token && prevId && nextId && prevId !== nextId) {
    scope = forkScope()
  }

  writeScope(scope, token, JSON.stringify(user ?? null), remember)
  return scope
}

/** 只清当前作用域；不会影响同浏览器里其他账号的登录态 */
export function clearSession() {
  migrateLegacy()
  const scope = currentScope()
  clearScope(scope)
}

/** 清指定作用域；会话清空后一并回收登记信息 */
export function clearScope(scope) {
  for (const store of [ls(), ss()]) {
    if (!store) continue
    store.removeItem(tokenKey(scope))
    store.removeItem(userKey(scope))
  }
  const stillHas = readScope(scope).token
  if (!stillHas) dropScope(scope)
}

/** 一次性清掉本浏览器所有作用域的会话（「退出全部设备」这类场景才用） */
export function clearAllSessions() {
  for (const store of [ls(), ss()]) {
    if (!store) continue
    const keys = []
    for (let i = 0; i < store.length; i += 1) {
      const key = store.key(i)
      if (key && key.startsWith('dr:s:')) keys.push(key)
    }
    keys.forEach((key) => store.removeItem(key))
    store.removeItem(LEGACY_TOKEN)
    store.removeItem(LEGACY_USER)
  }
}

function safeParse(raw) {
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

/** 记住上次登录的账号，用于登录页预填（只是文案提示，不涉及身份） */
export function rememberAccount(account) {
  if (account) ls()?.setItem('dr:last-account', account)
  else ls()?.removeItem('dr:last-account')
}

export function lastAccount() {
  return ls()?.getItem('dr:last-account') || ''
}

/* ------------------------------------------------------------------
 * 本机历史身份卡：登录成功后记录，最多保留 4 个，
 * 登录页以「身份卡芯片」的形式展示，点击即可一键填充。
 * ------------------------------------------------------------------ */
const HISTORY_KEY = 'dr:accounts'
const HISTORY_LIMIT = 4

export function historyAccounts() {
  try {
    const arr = JSON.parse(ls()?.getItem(HISTORY_KEY) || '[]')
    if (!Array.isArray(arr)) return []
    return arr.filter((a) => typeof a === 'string' && a).slice(0, HISTORY_LIMIT)
  } catch {
    return []
  }
}

export function pushHistoryAccount(account) {
  if (!account) return historyAccounts()
  const list = [account, ...historyAccounts().filter((a) => a !== account)].slice(0, HISTORY_LIMIT)
  ls()?.setItem(HISTORY_KEY, JSON.stringify(list))
  return list
}

export function removeHistoryAccount(account) {
  const list = historyAccounts().filter((a) => a !== account)
  ls()?.setItem(HISTORY_KEY, JSON.stringify(list))
  return list
}

export function clearHistoryAccounts() {
  ls()?.removeItem(HISTORY_KEY)
  return []
}
