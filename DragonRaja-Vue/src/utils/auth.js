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
