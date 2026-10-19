package com.fenglin.springboottest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 下发注册验证码请求（新生登记第 1 步）。
 * 前端 RegisterView.vue 提交字段：clientKey。
 *
 * <p>演示环境不发送邮件，验证码由后端直接回显给前端（"系统给验证码"）。</p>
 *
 * @author fenglin
 */
public class RegisterSendDTO {

    /** 浏览器端生成的会话标识（≤17 位字母数字），用于把验证码绑定到本次登记 */
    @NotBlank(message = "缺少浏览器标识，请刷新页面后重试")
    @Size(max = 17, message = "浏览器标识过长")
    private String clientKey;

    public RegisterSendDTO() {
    }

    public String getClientKey() {
        return clientKey;
    }

    public void setClientKey(String clientKey) {
        this.clientKey = clientKey;
    }
}
