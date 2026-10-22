package com.fenglin.springboottest.vo;

/**
 * 下发注册验证码返回。
 * 前端 RegisterView.vue 使用字段：demoCode / expireSeconds / cooldownSeconds。
 *
 * <p>演示环境把验证码直接回显给前端（系统给出、用户照填，不发邮件）；
 * 真实项目应改为短信 / 邮件下发，本 VO 只需下发成功标识即可。</p>
 *
 * @author fenglin
 */
public class RegisterCodeVO {

    /** 演示环境直接回显的 6 位验证码 */
    private String demoCode;

    /** 有效期（秒），5 分钟 = 300 */
    private Integer expireSeconds;

    /** 重新获取的冷却时间（秒），60 */
    private Integer cooldownSeconds;

    public RegisterCodeVO() {
    }

    public RegisterCodeVO(String demoCode, Integer expireSeconds, Integer cooldownSeconds) {
        this.demoCode = demoCode;
        this.expireSeconds = expireSeconds;
        this.cooldownSeconds = cooldownSeconds;
    }

    public String getDemoCode() {
        return demoCode;
    }

    public void setDemoCode(String demoCode) {
        this.demoCode = demoCode;
    }

    public Integer getExpireSeconds() {
        return expireSeconds;
    }

    public void setExpireSeconds(Integer expireSeconds) {
        this.expireSeconds = expireSeconds;
    }

    public Integer getCooldownSeconds() {
        return cooldownSeconds;
    }

    public void setCooldownSeconds(Integer cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
    }
}
