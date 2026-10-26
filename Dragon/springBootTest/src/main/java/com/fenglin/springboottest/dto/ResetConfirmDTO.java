package com.fenglin.springboottest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 校验验证龙文并重置密钥请求（密钥重置第 2 步）。
 * 前端 ForgotView.vue 提交字段：account / code / password。
 *
 * @author fenglin
 */
public class ResetConfirmDTO {

    /** 学号 或 行动代号（姓名） */
    @NotBlank(message = "请输入学号或姓名")
    private String account;

    /** 6 位数字验证龙文 */
    @NotBlank(message = "请输入验证龙文")
    @Pattern(regexp = "^\\d{6}$", message = "验证龙文应为 6 位数字")
    private String code;

    /** 新的言灵密钥 */
    @NotBlank(message = "请设置新的言灵密钥")
    @Size(min = 8, max = 64, message = "言灵密钥至少 8 位")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "言灵密钥需同时包含字母与数字")
    private String password;

    public ResetConfirmDTO() {
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
