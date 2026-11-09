package com.fenglin.springboottest.vo;

import java.io.Serializable;

/**
 * 收到的好友申请。
 *
 * @author fenglin
 */
public class FriendRequestVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** dr_friend_request.id，同意 / 拒绝时用 */
    private Long id;

    /** 申请人 dr_user.id */
    private Long fromUserId;

    private String account;
    private String codename;

    private String identity;
    private String identityName;

    /** 申请附言 */
    private String message;

    /** 申请时间（ISO 字符串） */
    private String createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(Long fromUserId) {
        this.fromUserId = fromUserId;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getCodename() {
        return codename;
    }

    public void setCodename(String codename) {
        this.codename = codename;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public String getIdentityName() {
        return identityName;
    }

    public void setIdentityName(String identityName) {
        this.identityName = identityName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
