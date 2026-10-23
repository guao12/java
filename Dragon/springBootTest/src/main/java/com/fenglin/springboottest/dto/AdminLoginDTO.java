package com.fenglin.springboottest.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

/**
 * 后台管理员登录请求。
 * 后台只有登录入口、没有注册入口，账号由 SQL 脚本预置。
 *
 * @author fenglin
 */
public class AdminLoginDTO {

    /** 管理员账号 */
    @NotBlank(message = "请输入管理员账号")
    @JsonAlias({"account", "username"})
    private String username;

    /** 登录密码 */
    @NotBlank(message = "请输入登录密码")
    private String password;

    /** 记住登录状态：true 时令牌有效期延长 */
    private Boolean remember = Boolean.FALSE;

    public AdminLoginDTO() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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
