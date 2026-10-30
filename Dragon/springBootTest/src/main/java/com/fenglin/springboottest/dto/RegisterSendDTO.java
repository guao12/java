package com.fenglin.springboottest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 下发注册验证码请求（新生登记第 1 步）。
 * 前端 RegisterView.vue 提交字段：clientKey / captchaToken / captchaCode。
 *
 * <p>演示环境不发送邮件，验证码由后端直接回显给前端（"系统给验证码"）。
 * 下发前必须通过图形验证码（captchaToken + captchaCode），且同一 IP 60 秒内仅允许一次，
 * 以此杜绝"换 clientKey 绕过冷却、脚本批量刷取"的漏洞。</p>
 *
 * @author fenglin
 */
public class RegisterSendDTO {

    /** 浏览器端生成的会话标识（≤17 位字母数字），用于把验证码绑定到本次登记 */
    @NotBlank(message = "缺少浏览器标识，请刷新页面后重试")
    @Size(max = 17, message = "浏览器标识过长")
    private String clientKey;

    /** 图形验证码令牌（由 GET /api/auth/captcha 下发，服务端签发、不可伪造） */
    @NotBlank(message = "缺少图形验证码，请刷新后重试")
    private String captchaToken;

    /** 用户照图形验证码填入的文字 */
    @NotBlank(message = "请输入图形验证码")
    private String captchaCode;

    public RegisterSendDTO() {
    }

    public String getClientKey() {
        return clientKey;
    }

    public void setClientKey(String clientKey) {
        this.clientKey = clientKey;
    }

    public String getCaptchaToken() {
        return captchaToken;
    }

    public void setCaptchaToken(String captchaToken) {
        this.captchaToken = captchaToken;
    }

    public String getCaptchaCode() {
        return captchaCode;
    }

    public void setCaptchaCode(String captchaCode) {
        this.captchaCode = captchaCode;
    }
}
