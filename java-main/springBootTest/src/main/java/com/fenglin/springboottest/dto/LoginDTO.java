package com.fenglin.springboottest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 登录请求体。
 *
 * @author fenglin
 */
public class LoginDTO {

    @NotBlank(message = "骑士名不能为空")
    @Size(max = 50, message = "骑士名最长 50 个字符")
    private String username;

    @NotBlank(message = "变身密码不能为空")
    @Size(min = 6, max = 64, message = "变身密码长度需为 6~64 位")
    private String password;

    /** 前端"记住此骑士"勾选项，仅回传给前端用于本地存储，后端不做额外处理 */
    private Boolean remember = Boolean.FALSE;

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
