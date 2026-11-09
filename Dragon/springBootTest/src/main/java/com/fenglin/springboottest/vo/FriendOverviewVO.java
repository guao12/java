package com.fenglin.springboottest.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 好友页概览：一次请求拿齐首屏需要的东西。
 *
 * <p>刻意做成"一个大对象"而不是四五个小接口 —— 好友页是高频入口，
 * 首屏少发几个 RTT 的收益比接口分离的洁癖更实在。</p>
 *
 * @author fenglin
 */
public class FriendOverviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 好友总数 */
    private Integer friendCount;

    /** 待我处理的好友申请数 */
    private Integer pendingCount;

    /** 我的未读私信总数 */
    private Integer unreadCount;

    /** 待处理申请（最多 20 条） */
    private List<FriendRequestVO> requests;

    /** 好友列表（按最近会话排序，最多 20 条） */
    private List<FriendVO> friends;

    public Integer getFriendCount() {
        return friendCount;
    }

    public void setFriendCount(Integer friendCount) {
        this.friendCount = friendCount;
    }

    public Integer getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(Integer pendingCount) {
        this.pendingCount = pendingCount;
    }

    public Integer getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(Integer unreadCount) {
        this.unreadCount = unreadCount;
    }

    public List<FriendRequestVO> getRequests() {
        return requests;
    }

    public void setRequests(List<FriendRequestVO> requests) {
        this.requests = requests;
    }

    public List<FriendVO> getFriends() {
        return friends;
    }

    public void setFriends(List<FriendVO> friends) {
        this.friends = friends;
    }
}
