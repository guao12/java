package com.fenglin.springboottest.vo;

import java.io.Serializable;

/**
 * 导航角标用的轻量数字：只返回两个计数，别让它把整个概览都拖下来。
 *
 * @author fenglin
 */
public class FriendBadgeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 未读私信数 */
    private Integer unread;

    /** 待处理好友申请数 */
    private Integer pending;

    public FriendBadgeVO() {
    }

    public FriendBadgeVO(Integer unread, Integer pending) {
        this.unread = unread;
        this.pending = pending;
    }

    public Integer getUnread() {
        return unread;
    }

    public void setUnread(Integer unread) {
        this.unread = unread;
    }

    public Integer getPending() {
        return pending;
    }

    public void setPending(Integer pending) {
        this.pending = pending;
    }
}
