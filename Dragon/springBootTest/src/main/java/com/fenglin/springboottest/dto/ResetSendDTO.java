package com.fenglin.springboottest.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 下发验证龙文请求（密钥重置第 1 步）。
 * 前端 ForgotView.vue 提交字段：account。
 *
 * @author fenglin
 */
public class ResetSendDTO {

    /** 学号 / 学院邮箱 */
    @NotBlank(message = "请输入学号 / 邮箱")
    private String account;

    public ResetSendDTO() {
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }
}
