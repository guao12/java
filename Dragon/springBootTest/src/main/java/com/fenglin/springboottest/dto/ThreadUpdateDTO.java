package com.fenglin.springboottest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * 编辑主题参数。
 *
 * <p>只允许改标题 / 正文 / 密级 / 回复可见，版块不给改 ——
 * 帖子挪版块会让原有讨论脱离上下文，真有需要让版主处理。</p>
 *
 * @author fenglin
 */
public class ThreadUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "标题不能为空")
    @Size(min = 4, max = 80, message = "标题需 4–80 字")
    private String title;

    @NotBlank(message = "正文不能为空")
    @Size(min = 10, max = 8000, message = "正文需 10–8000 字")
    private String content;

    /** 密级，null 表示不改 */
    private Integer securityLevel;

    /** 回复可见，null 表示不改 */
    private Boolean replyVisible;

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
