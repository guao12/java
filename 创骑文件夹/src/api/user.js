import { http } from './request.js'

/**
 * 骑士（用户）相关接口，与后端 com.fenglin.springboottest.controller.UserController 一一对应。
 */

/** 后端健康检查，用于展示"后端连接状态" */
export function health() {
  return http.get('/health')
}

/** 登录：{ username, password, remember } */
export function login({ username, password, remember = false }) {
  return http.post('/user/login', { username, password, remember })
}

/** 注册：{ username, email, password, confirmPassword, formKey } */
export function register({ username, email, password, confirmPassword, formKey = 'rabbittank' }) {
  return http.post('/user/register', { username, email, password, confirmPassword, formKey })
}

/** 退出登录 */
export function logout() {
  return http.post('/user/logout')
}

/** 当前登录用户，未登录时后端返回 401 */
export function me() {
  return http.get('/user/me')
}

/** 骑士名是否可用 */
export function checkUsername(username) {
  return http.get('/user/check-username', { username })
}

/** 骑士花名册 */
export function listUsers() {
  return http.get('/user/list')
}
