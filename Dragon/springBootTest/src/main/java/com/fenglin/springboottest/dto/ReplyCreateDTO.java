package com.fenglin.springboottest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 批注（回帖）请求。
 *
 * @author fenglin
 */
public class ReplyCreateDTO {

    @NotBlank(message = "回复内容不能为空")
    @Size(min = 2, max = 2000, message = "回复需 2–2000 字")
    private String content;

    /** 引用的批注 id，可不填 */
    private Long quoteId;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getQuoteId() {
        return quoteId;
    }

    public void setQuoteId(Long quoteId) {
        this.quoteId = quoteId;
    }
}
