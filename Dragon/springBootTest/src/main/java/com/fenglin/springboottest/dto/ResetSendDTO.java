package com.fenglin.springboottest.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 下发验证龙文请求（密钥重置第 1 步）。
 * 前端 ForgotView.vue 提交字段：account（学号或行动代号/姓名，后端二者均可）。
 *
 * @author fenglin
 */
public class ResetSendDTO {

    /** 学号 或 行动代号（姓名） */
    @NotBlank(message = "请输入学号或姓名")
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
