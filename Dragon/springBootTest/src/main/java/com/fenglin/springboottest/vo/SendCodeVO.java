package com.fenglin.springboottest.vo;

/**
 * 下发验证龙文返回。
 *
 * <p>是否带回验证码由下发通道决定：</p>
 * <ul>
 *   <li>{@code log}（生产默认）：demoCode 恒为 null，验证码只写日志，需接入短信 / 邮件才真正送达；</li>
 *   <li>{@code echo}（{@code dr.auth.reset-code-echo=true}，仅本地演示）：demoCode 直接返回，前端展示给用户照填。</li>
 * </ul>
 * <p>未登记账号在 echo 通道下也会返回一个假码，保证响应形状一致，避免用「有没有 demoCode」探测账号存在性。</p>
 *
 * @author fenglin
 */
public class SendCodeVO {

    /** 验证码所属的学院编号（学号），前端用于回显确认 */
    private String account;

    /** 验证码：echo 通道下为真实值（或未登记账号的假码），log 通道下恒为 null */
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