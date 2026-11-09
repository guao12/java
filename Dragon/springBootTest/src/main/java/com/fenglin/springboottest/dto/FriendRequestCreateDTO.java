package com.fenglin.springboottest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 发送好友申请。
 *
 * @author fenglin
 */
public class FriendRequestCreateDTO {

    /** 目标用户 dr_user.id */
    @NotNull(message = "请选择要添加的对象")
    private Long toUserId;

    /** 申请附言，可不填 */
    @Size(max = 120, message = "附言最多 120 字")
    private String message;

    public Long getToUserId() {
        return toUserId;
    }

    public void setToUserId(Long toUserId) {
        this.toUserId = toUserId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
