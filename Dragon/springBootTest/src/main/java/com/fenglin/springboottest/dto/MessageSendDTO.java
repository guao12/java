package com.fenglin.springboottest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 发送私信。
 *
 * @author fenglin
 */
public class MessageSendDTO {

    /** 收件人 dr_user.id */
    @NotNull(message = "缺少收件人")
    private Long toUserId;

    @NotBlank(message = "消息不能为空")
    @Size(max = 500, message = "单条消息最多 500 字")
    private String content;

    public Long getToUserId() {
        return toUserId;
    }

    public void setToUserId(Long toUserId) {
        this.toUserId = toUserId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
