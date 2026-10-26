package com.fenglin.springboottest.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

/**
 * 身份认证（登录）请求。
 * 前端 LoginView.vue 提交字段：account / password / remember。
 * account 为学院编号（学号，如 CC20260001）或行动代号（姓名）；@JsonAlias 兼容 username 字段名。
 * 登录支持学号或姓名（邮箱已全链路移除）。
 *
 * @author fenglin
 */
public class LoginDTO {

    /** 学院编号（学号）或行动代号（姓名） */
    @NotBlank(message = "请输入学号或姓名")
    @JsonAlias({"username"})
    private String account;

    /** 言灵密钥 */
    @NotBlank(message = "请输入言灵密钥")
    private String password;

    /** 记住身份卡：true 时令牌有效期延长 */
    private Boolean remember = Boolean.FALSE;

    public LoginDTO() {
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getRemember() {
        return remember;
    }

    public void setRemember(Boolean remember) {
        this.remember = remember;
    }
}
