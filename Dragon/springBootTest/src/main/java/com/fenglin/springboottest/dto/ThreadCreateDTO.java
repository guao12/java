package com.fenglin.springboottest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 发帖请求。
 *
 * <p>长度限制与数据库列宽对齐（标题 120 / 正文 mediumtext），这里收得更紧：
 * 标题 4–80 字、正文 10–8000 字。太短的标题在列表里无法辨认，
 * 太长的正文既拖慢列表摘要，也不是论坛该有的形态（长文应放档案室）。</p>
 *
 * @author fenglin
 */
public class ThreadCreateDTO {

    /** 版块 code，如 canon / fanwork */
    @NotBlank(message = "请选择版块")
    @Size(max = 32, message = "版块标识不合法")
    private String boardCode;

    @NotBlank(message = "标题不能为空")
    @Size(min = 4, max = 80, message = "标题需 4–80 字")
    private String title;

    @NotBlank(message = "正文不能为空")
    @Size(min = 10, max = 8000, message = "正文需 10–8000 字")
    private String content;

    /** 1 公开 2 内部 3 机密 4 绝密；为空按 1 处理 */
    private Integer securityLevel;

    /** 是否「回复后可见」 */
    private Boolean replyVisible;

    public String getBoardCode() {
        return boardCode;
    }

    public void setBoardCode(String boardCode) {
        this.boardCode = boardCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getSecurityLevel() {
        return securityLevel;
    }

    public void setSecurityLevel(Integer securityLevel) {
        this.securityLevel = securityLevel;
    }

    public Boolean getReplyVisible() {
        return replyVisible;
    }

    public void setReplyVisible(Boolean replyVisible) {
        this.replyVisible = replyVisible;
    }
}
