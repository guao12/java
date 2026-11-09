package com.fenglin.springboottest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 新生登记（注册）请求。
 * 前端 RegisterView.vue 提交字段：codename / identity / password / captchaToken / captchaCode。
 *
 * <p>登记前必须通过图形验证码校验（captchaToken + captchaCode），
 * 取代原先「下发 6 位数字验证码」的流程：图形验证码由服务端签发、一次性消费，无需短信 / 邮件。</p>
 *
 * <p>2026-11 起注册页不再采集言灵：{@code spirit} 字段保留（接口直调 / 老客户端仍可用），
 * 前端不提交，服务端对空值默认写 {@code none}（未觉醒），血统鉴定挪到 /appraisal 页。</p>
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

    /** 图形验证码令牌（由 GET /api/auth/captcha 下发，服务端签发、不可伪造） */
    @NotBlank(message = "缺少图形验证码，请刷新后重试")
    private String captchaToken;

    /** 用户照图形验证码填入的文字 */
    @NotBlank(message = "请输入图形验证码")
    private String captchaCode;

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
