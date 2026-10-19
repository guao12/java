package com.fenglin.springboottest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 新生登记（注册）请求。
 * 前端 RegisterView.vue 提交字段：codename / identity / spirit / password / code / clientKey。
 *
 * <p>登记前必须先通过验证码校验：验证码由 /api/auth/register/send 下发，
 * 演示环境直接回显给前端（不再使用邮箱验证）。</p>
 *
 * @author fenglin
 */
public class RegisterDTO {

    /** 姓名 / 行动代号，2-16 字符 */
    @NotBlank(message = "请输入姓名或行动代号")
    @Size(min = 2, max = 16, message = "姓名 / 行动代号长度需在 2-16 个字符之间")
    private String codename;

    /** 在校身份 code */
    @NotBlank(message = "请选择在校身份")
    private String identity;

    /** 言灵 code（可为空，默认 none） */
    private String spirit;

    /** 言灵密钥：至少 8 位，同时包含字母与数字 */
    @NotBlank(message = "请设置言灵密钥")
    @Size(min = 8, max = 64, message = "言灵密钥至少 8 位")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "言灵密钥需同时包含字母与数字")
    private String password;

    /** 6 位数字验证码（由系统下发并回显） */
    @NotBlank(message = "请输入验证码")
    @Pattern(regexp = "^\\d{6}$", message = "验证码应为 6 位数字")
    private String code;

    /** 浏览器端生成的会话标识，与下发验证码时使用的保持一致 */
    @NotBlank(message = "缺少浏览器标识，请刷新页面后重试")
    @Size(max = 17, message = "浏览器标识过长")
    private String clientKey;

    public RegisterDTO() {
    }

    public String getCodename() {
        return codename;
    }

    public void setCodename(String codename) {
        this.codename = codename;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public String getSpirit() {
        return spirit;
    }

    public void setSpirit(String spirit) {
        this.spirit = spirit;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getClientKey() {
        return clientKey;
    }

    public void setClientKey(String clientKey) {
        this.clientKey = clientKey;
    }
}
