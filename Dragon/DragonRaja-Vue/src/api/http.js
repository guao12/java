import { createRequest, CODE_UNAUTHORIZED } from '@/api/request'
import { clearSession, getToken } from '@/utils/auth'

export { CODE_UNAUTHORIZED }

/** 业务异常：message 直接取自后端返回，可直接展示给用户 */
export class ApiError extends Error {
  constructor(message, code) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

/**
 * 学员端请求：使用学员令牌（dr:token），401 时清理学员会话。
 * 具体流程见 api/request.js 的 createRequest（与后台共用同一套实现）。
 */
export const request = createRequest({
  getToken,
  onUnauthorized: clearSession,
  ErrorClass: ApiError,
  unauthorizedMessage: '登录状态已失效，请重新认证'
})
