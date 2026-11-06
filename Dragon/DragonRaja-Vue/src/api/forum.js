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

  /** 主题详情 + 第 1 页楼层 */
  thread(id) {
    return request(`/api/forum/threads/${id}`, { auth: true, cacheTtl: 0 })
  },

  /**
   * 楼层分页。
   * 详情接口只带第 1 页，翻页走这里；不做缓存，保证刚发的楼层立刻能取到。
   * @param {number} page 从 1 开始
   */
  replies(id, page = 1, size = 20) {
    return request(`/api/forum/threads/${id}/replies${toQuery({ page, size })}`, {
      auth: true,
      cacheTtl: 0
    })
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

  /**
   * 举报主题或楼层。
   * 同一人对同一对象只能举报一次，重复提交后端会返回提示。
   * @param {{targetType:'thread'|'reply', targetId:number, threadId:number, reason:string, detail?:string}} body
   */
  report(body) {
    return request('/api/forum/reports', { method: 'POST', body })
  },

  /** 收藏 / 取消收藏 */
  collect(id) {
    return request(`/api/forum/threads/${id}/collect`, {
      method: 'POST',
      invalidate: '/api/forum'
    })
  },

  /** 我的收藏（按收藏时间倒序，需登录） */
  collected(params = {}) {
    return request(`/api/forum/collected${toQuery(params)}`, { auth: true, cacheTtl: 0 })
  },

  /* ---------------- 编辑 / 删除 ---------------- */

  /** 编辑主题（仅作者本人）。返回编辑后的详情 */
  updateThread(id, body) {
    return request(`/api/forum/threads/${id}`, {
      method: 'PUT',
      body,
      invalidate: '/api/forum'
    })
  },

  /** 删除主题（作者本人或版务）。逻辑删除，楼层保留 */
  deleteThread(id, reason = '') {
    return request(`/api/forum/threads/${id}${toQuery({ reason })}`, {
      method: 'DELETE',
      invalidate: '/api/forum'
    })
  },

  /** 编辑楼层（仅该层作者）。返回编辑后的楼层 */
  updateReply(threadId, replyId, body) {
    return request(`/api/forum/threads/${threadId}/replies/${replyId}`, {
      method: 'PUT',
      body,
      invalidate: '/api/forum'
    })
  },

  /** 删除楼层（该层作者或版务） */
  deleteReply(threadId, replyId) {
    return request(`/api/forum/threads/${threadId}/replies/${replyId}`, {
      method: 'DELETE',
      invalidate: '/api/forum'
    })
  },

  /** 论坛内检索（标题 / 正文 / 作者） */
  search(kw, limit = 8) {
    return request(`/api/forum/search${toQuery({ kw, limit })}`, { auth: true })
  }
}
