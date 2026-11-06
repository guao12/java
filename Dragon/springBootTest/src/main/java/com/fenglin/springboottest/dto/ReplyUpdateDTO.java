package com.fenglin.springboottest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * 编辑楼层参数。
 *
 * <p>只给改正文：楼层号、引用关系、作者都是事实记录，不允许事后改写。</p>
 *
 * @author fenglin
 */
public class ReplyUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "回复内容不能为空")
    @Size(min = 2, max = 2000, message = "回复需 2–2000 字")
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
