package com.fenglin.springboottest.vo;

/**
 * 下发验证龙文返回。
 * 安全修复：验证码不再回显给前端（demoCode 恒为 null），仅写入后端日志；
 * 真实项目应改为短信 / 站内信下发。前端 ForgotView.vue 使用字段：account / demoCode / expireSeconds。
 *
 * @author fenglin
 */
public class SendCodeVO {

    /** 验证码所属的学院编号（学号），前端用于回显确认 */
    private String account;

    /** 已废弃：安全修复后不再回显验证码，恒为 null（验证码仅写入后端日志） */
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