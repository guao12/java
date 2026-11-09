package com.fenglin.springboottest.vo;

import java.io.Serializable;

/**
 * 一条私信。
 *
 * <p>{@code mine} 由服务端按当前登录者算好：前端不必再比对 senderId，
 * 气泡左右分栏只看这一个布尔值。</p>
 *
 * @author fenglin
 */
public class MessageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long senderId;
    private Long receiverId;
    private String content;

    /** 是否我发出的 */
    private boolean mine;

    /** 发送时间（ISO 字符串） */
    private String createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isMine() {
        return mine;
    }

    public void setMine(boolean mine) {
        this.mine = mine;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
