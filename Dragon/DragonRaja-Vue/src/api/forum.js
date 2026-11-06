import { request } from './http'
import { toQuery } from './request'

/**
 * 论坛接口。
 * 后端：springBootTest · ForumController（/api/forum/**）
 *
 * 读接口挂的是软鉴权：带令牌就按身份过滤密级，不带就只看公开级。
 * 写接口（发帖 / 回帖 / 点赞）必须登录，未登录返回 code=401。
 *
 * 缓存策略：
 *  - 概览与列表允许 3 秒缓存（切页面回来不闪白）
 *  - 详情 cacheTtl=0：后端每次详情会 +1 浏览，缓存会让浏览数失真
 *  - 写操作统一失效 /api/forum 前缀，避免发完帖列表还是旧的
 */
export const forumApi = {
  /** 首页概览：版块 + 最新 + 最热 + 统计 + 我的权限 */
  overview() {
    return request('/api/forum/overview', { auth: true })
  },

  /**
   * 版块分页列表。
   * @param {{board:string, sort?:string, kw?:string, page?:number, size?:number}} params
   */
  threads(params) {
    return request(`/api/forum/threads${toQuery(params)}`, { auth: true })
  },

  /** 主题详情 + 楼层 */
  thread(id) {
    return request(`/api/forum/threads/${id}`, { auth: true, cacheTtl: 0 })
  },

  /** 发帖 */
  create(body) {
    return request('/api/forum/threads', {
      method: 'POST',
      body,
      invalidate: '/api/forum'
    })
  },

  /** 回帖 */
  reply(id, body) {
    return request(`/api/forum/threads/${id}/replies`, {
      method: 'POST',
      body,
      invalidate: '/api/forum'
    })
  },

  /** 点赞 / 取消点赞 */
  like(id) {
    return request(`/api/forum/threads/${id}/like`, {
      method: 'POST',
      invalidate: '/api/forum'
    })
  },

  /** 论坛内检索（标题 / 正文 / 作者） */
  search(kw, limit = 8) {
    return request(`/api/forum/search${toQuery({ kw, limit })}`, { auth: true })
  }
}
