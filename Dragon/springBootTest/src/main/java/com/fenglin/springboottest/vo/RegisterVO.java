package com.fenglin.springboottest.vo;

/**
 * 新生登记（注册）成功返回：后端签发的学院编号 + 档案摘要。
 * 前端 RegisterView.vue 使用字段：account / codename / identityName / spiritName。
 *
 * @author fenglin
 */
public class RegisterVO {

    /** 学院编号（学号），如 CC20260001 */
    private String account;

    /** 姓名 / 行动代号 */
    private String codename;

    /** 在校身份中文名 */
    private String identityName;

    /** 言灵中文名 */
    private String spiritName;

    public RegisterVO() {
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getCodename() {
        return codename;
    }

    public void setCodename(String codename) {
        this.codename = codename;
    }

    public String getIdentityName() {
        return identityName;
    }

    public void setIdentityName(String identityName) {
        this.identityName = identityName;
    }

    public String getSpiritName() {
        return spiritName;
    }

    public void setSpiritName(String spiritName) {
        this.spiritName = spiritName;
    }
}
