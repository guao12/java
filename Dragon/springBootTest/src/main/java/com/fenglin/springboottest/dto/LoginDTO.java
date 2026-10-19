package com.fenglin.springboottest.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

/**
 * 身份认证（登录）请求。
 * 前端 LoginView.vue 提交字段：account / password / remember。
 * account 支持学号（CC20260001）、学院邮箱；@JsonAlias 兼容 username 字段名。
 *
 * @author fenglin
 */
public class LoginDTO {

    /** 学号 / 学院邮箱 */
    @NotBlank(message = "请输入学号 / 邮箱 / 执行部编号")
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
