package com.fenglin.springboottest.vo;

/**
 * 下发验证龙文返回。
 * 前端 ForgotView.vue 使用字段：account / demoCode / expireSeconds。
 * 演示环境把验证码直接回显给前端；真实项目应改为短信 / 站内信下发。
 *
 * @author fenglin
 */
public class SendCodeVO {

    /** 验证码所属的学院编号（学号），前端用于回显确认 */
    private String account;

    /** 演示环境直接回显的 6 位验证码 */
    private String demoCode;

    /** 有效期（秒），5 分钟 = 300 */
    private Integer expireSeconds;

    public SendCodeVO() {
    }

    public SendCodeVO(String account, String demoCode, Integer expireSeconds) {
        this.account = account;
        this.demoCode = demoCode;
        this.expireSeconds = expireSeconds;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
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
}