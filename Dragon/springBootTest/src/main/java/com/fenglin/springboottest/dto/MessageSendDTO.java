package com.fenglin.springboottest.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

    /**
     * 消息类型：0=文本（默认） 1=表情包。
     *
     * <p>表情包时 {@code content} 传表情编码（如 s01），服务端会校验编码是否存在。</p>
     */
    @Min(value = 0, message = "消息类型不合法")
    @Max(value = 1, message = "消息类型不合法")
    private Integer msgType;

    public Long getToUserId() {
        return toUserId;
    }

    public void setToUserId(Long toUserId) {
        this.toUserId = toUserId;
    }

    public Integer getMsgType() {
        return msgType;
    }

    public void setMsgType(Integer msgType) {
        this.msgType = msgType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
