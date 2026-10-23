/**
 * 后台管理员会话管理。
 * 与学员会话（utils/auth.js）完全隔离：
 *  - 后台令牌存 dr:admin:token，清退后台不影响学员登录态，反之亦然
 *  - 学员令牌进不了后台接口，后台令牌也进不了学员接口（后端 scope=admin 双向隔离）
 */

const TOKEN_KEY = 'dr:admin:token'
const NAME_KEY = 'dr:admin:name'

function readStores(key) {
  return sessionStorage.getItem(key) ?? localStorage.getItem(key)
}

export function getAdminToken() {
  return readStores(TOKEN_KEY)
}

/** 管理员显示名（如「执行部管理员」） */
export function getAdminName() {
  return readStores(NAME_KEY)
}

export function isAdminLoggedIn() {
  return !!getAdminToken()
}

/** 保存后台会话；remember = true 时写入 localStorage */
export function saveAdminSession({ token, admin }, remember = false) {
  clearAdminSession()
  const store = remember ? localStorage : sessionStorage
  store.setItem(TOKEN_KEY, token)
  store.setItem(NAME_KEY, admin?.nickname || admin?.username || '管理员')
}

export function clearAdminSession() {
  for (const store of [localStorage, sessionStorage]) {
    store.removeItem(TOKEN_KEY)
    store.removeItem(NAME_KEY)
  }
}
