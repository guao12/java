package com.fenglin.springboottest.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 下发验证龙文请求（密钥重置第 1 步）。
 * 前端 ForgotView.vue 提交字段：account / captchaToken / captchaCode。
 *
 * <p>下发前必须通过图形验证码，且同一 IP 60 秒内仅允许一次，
 * 以此杜绝"脚本批量探测账号是否已注册 / 刷取验证龙文"。</p>
 *
 * @author fenglin
 */
public class ResetSendDTO {

    /** 学号 或 行动代号（姓名） */
    @NotBlank(message = "请输入学号或姓名")
    private String account;

    /** 图形验证码令牌（由 GET /api/auth/captcha 下发，服务端签发、不可伪造） */
    @NotBlank(message = "缺少图形验证码，请刷新后重试")
    private String captchaToken;

    /** 用户照图形验证码填入的文字 */
    @NotBlank(message = "请输入图形验证码")
    private String captchaCode;

    public ResetSendDTO() {
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
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
