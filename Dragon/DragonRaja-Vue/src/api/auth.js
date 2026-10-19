import { request } from './http'

/**
 * 认证相关接口，与后端 AuthController / ProfileController 一一对应。
 */
export const authApi = {
  /** 新生登记 */
  register: (payload) => request('/api/auth/register', { method: 'POST', body: payload, auth: false }),

  /** 下发注册验证码（演示环境由系统直接给出验证码，不发邮件） */
  sendRegisterCode: (clientKey) =>
    request('/api/auth/register/send', { method: 'POST', body: { clientKey }, auth: false }),

  /** 身份认证 */
  login: (payload) => request('/api/auth/login', { method: 'POST', body: payload, auth: false }),

  /** 账号是否已登记 */
  checkAccount: (account) =>
    request(`/api/auth/check-account?account=${encodeURIComponent(account)}`, { auth: false }),

  /** 下发密钥重置验证龙文 */
  sendResetCode: (account) =>
    request('/api/auth/reset/send', { method: 'POST', body: { account }, auth: false }),

  /** 校验验证龙文并重置密钥 */
  confirmReset: (payload) =>
    request('/api/auth/reset/confirm', { method: 'POST', body: payload, auth: false }),

  /** 前后端共用字典 */
  dictionaries: () => request('/api/auth/dictionaries', { auth: false }),

  /** 我的身份卡（需登录） */
  profile: () => request('/api/user/profile'),

  /** 退出登录 */
  logout: () => request('/api/user/logout', { method: 'POST' })
}
