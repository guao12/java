package com.fenglin.springboottest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 注册请求体。
 *
 * @author fenglin
 */
public class RegisterDTO {

    @NotBlank(message = "骑士名不能为空")
    @Size(min = 2, max = 50, message = "骑士名长度需为 2~50 个字符")
    private String username;

    @NotBlank(message = "召集令邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱最长 100 个字符")
    private String email;

    @NotBlank(message = "变身密码不能为空")
    @Size(min = 6, max = 64, message = "变身密码长度需为 6~64 位")
    private String password;

    /** 前端二次确认密码，后端再校验一次，防止绕过前端直接调接口 */
    @NotBlank(message = "请再次输入变身密码")
    private String confirmPassword;

    /** 当前选择的形态，可选，默认 rabbittank */
    private String formKey;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getFormKey() {
        return formKey;
    }

    public void setFormKey(String formKey) {
        this.formKey = formKey;
    }
}
