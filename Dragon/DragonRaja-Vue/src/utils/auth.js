/**
 * 会话管理：JWT 令牌 + 用户信息缓存。
 * 约定：令牌存 localStorage（勾选"记住身份卡"时），否则存 sessionStorage。
 */

const TOKEN_KEY = 'dr:token'
const USER_KEY = 'dr:user'

function readStores(key) {
  return sessionStorage.getItem(key) ?? localStorage.getItem(key)
}

export function getToken() {
  return readStores(TOKEN_KEY)
}

export function getUser() {
  const raw = readStores(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

export function isLoggedIn() {
  return !!getToken()
}

/** 保存会话；remember = true 时写入 localStorage，浏览器关闭后仍保留 */
export function saveSession({ token, user }, remember = true) {
  clearSession()
  const store = remember ? localStorage : sessionStorage
  store.setItem(TOKEN_KEY, token)
  store.setItem(USER_KEY, JSON.stringify(user ?? null))
}

export function clearSession() {
  for (const store of [localStorage, sessionStorage]) {
    store.removeItem(TOKEN_KEY)
    store.removeItem(USER_KEY)
  }
}

/** 记住上次登录的账号，用于登录页预填 */
export function rememberAccount(account) {
  if (account) localStorage.setItem('dr:last-account', account)
  else localStorage.removeItem('dr:last-account')
}

export function lastAccount() {
  return localStorage.getItem('dr:last-account') || ''
}

/* ------------------------------------------------------------------
 * 本机历史身份卡：登录成功后记录，最多保留 4 个，
 * 登录页以「身份卡芯片」的形式展示，点击即可一键填充。
 * ------------------------------------------------------------------ */
const HISTORY_KEY = 'dr:accounts'
const HISTORY_LIMIT = 4

export function historyAccounts() {
  try {
    const arr = JSON.parse(localStorage.getItem(HISTORY_KEY) || '[]')
    if (!Array.isArray(arr)) return []
    return arr.filter((a) => typeof a === 'string' && a).slice(0, HISTORY_LIMIT)
  } catch {
    return []
  }
}

export function pushHistoryAccount(account) {
  if (!account) return historyAccounts()
  const list = [account, ...historyAccounts().filter((a) => a !== account)].slice(0, HISTORY_LIMIT)
  localStorage.setItem(HISTORY_KEY, JSON.stringify(list))
  return list
}

export function removeHistoryAccount(account) {
  const list = historyAccounts().filter((a) => a !== account)
  localStorage.setItem(HISTORY_KEY, JSON.stringify(list))
  return list
}

export function clearHistoryAccounts() {
  localStorage.removeItem(HISTORY_KEY)
  return []
}
