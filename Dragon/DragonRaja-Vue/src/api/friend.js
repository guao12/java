import { request } from './http'
import { toQuery } from './request'

/**
 * 好友 / 私信接口。
 * 后端：springBootTest · FriendController（/api/friend/**）
 *
 * 与论坛不同，这里全部接口都要求登录：好友和私信没有游客场景，
 * 未登录时后端直接返回 401，api 层会清掉会话并跳登录页。
 *
 * 缓存策略：
 *  - 概览 / 好友列表允许 3 秒缓存（切页面回来不闪白）
 *  - 会话消息 cacheTtl=0：聊天必须实时，缓存会导致刚发的消息看不见
 *  - 所有写操作失效 /api/friend 前缀，避免加完好友列表还是旧的
 */
export const friendApi = {
  /** 概览：好友数 / 待处理申请数 / 未读数 + 申请列表 + 好友列表 */
  overview() {
    return request('/api/friend/overview', { auth: true })
  },

  /** 导航角标（轮询用，返回体很小） */
  badge() {
    return request('/api/friend/badge', { auth: true, cacheTtl: 0 })
  },

  /** 搜人：学号或行动代号模糊匹配 */
  search(kw) {
    return request(`/api/friend/search${toQuery({ kw })}`, { auth: true, cacheTtl: 0 })
  },

  /** 我的好友（按最近会话排序） */
  friends() {
    return request('/api/friend/list', { auth: true })
  },

  /** 我收到的待处理申请 */
  requests() {
    return request('/api/friend/requests', { auth: true, cacheTtl: 0 })
  },

  /** 发送好友申请 */
  sendRequest(body) {
    return request('/api/friend/requests', {
      method: 'POST',
      body,
      invalidate: '/api/friend'
    })
  },

  /** 同意申请 */
  accept(id) {
    return request(`/api/friend/requests/${id}/accept`, {
      method: 'POST',
      invalidate: '/api/friend'
    })
  },

  /** 拒绝申请 */
  reject(id) {
    return request(`/api/friend/requests/${id}/reject`, {
      method: 'POST',
      invalidate: '/api/friend'
    })
  },

  /** 解除好友 */
  remove(userId) {
    return request(`/api/friend/${userId}`, {
      method: 'DELETE',
      invalidate: '/api/friend'
    })
  },

  /**
   * 会话历史（时间正序）。
   * @param {{friendId:number, beforeId?:number, size?:number}} params
   */
  messages(params) {
    return request(`/api/friend/messages${toQuery(params)}`, { auth: true, cacheTtl: 0 })
  },

  /** 增量拉新消息：id 大于 afterId 的，时间正序（轮询用） */
  newMessages(friendId, afterId) {
    return request(`/api/friend/messages/new${toQuery({ friendId, afterId })}`, {
      auth: true,
      cacheTtl: 0
    })
  },

  /** 发私信 */
  send(body) {
    return request('/api/friend/messages', {
      method: 'POST',
      body,
      invalidate: '/api/friend'
    })
  },

  /** 会话标记已读，返回剩余未读总数 */
  read(friendId) {
    return request(`/api/friend/read${toQuery({ friendId })}`, {
      method: 'POST',
      invalidate: '/api/friend'
    })
  }
}
