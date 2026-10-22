package com.fenglin.springboottest.vo;

/**
 * 账号是否已登记返回。
 * 前端 LoginView.vue / ForgotView.vue 使用字段：registered / codename。
 *
 * @author fenglin
 */
public class CheckAccountVO {

    /** 该身份卡是否已在册 */
    private Boolean registered;

    /** 在册者的姓名 / 行动代号，便于用户确认没输错学号 */
    private String codename;

    public CheckAccountVO() {
    }

    public CheckAccountVO(Boolean registered, String codename) {
        this.registered = registered;
        this.codename = codename;
    }

    public Boolean getRegistered() {
        return registered;
    }

    public void setRegistered(Boolean registered) {
        this.registered = registered;
    }

    public String getCodename() {
        return codename;
    }

    public void setCodename(String codename) {
        this.codename = codename;
    }
}