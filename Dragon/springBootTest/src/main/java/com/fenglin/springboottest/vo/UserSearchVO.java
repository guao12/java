package com.fenglin.springboottest.vo;

import java.io.Serializable;

/**
 * 用户搜索结果项。
 *
 * <p>{@code relation} 让前端一个按钮就够用：
 * none → 「加好友」，pending_out → 「已申请」（禁用），
 * pending_in → 「同意」，friend → 「已是好友」（禁用），self → 不展示。</p>
 *
 * @author fenglin
 */
public class UserSearchVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String account;
    private String codename;

    private String identity;
    private String identityName;

    private String spirit;
    private String spiritName;

    /** none / pending_out / pending_in / friend */
    private String relation;

    /** 关系的中文说明，可直接展示 */
    private String relationLabel;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public String getIdentityName() {
        return identityName;
    }

    public void setIdentityName(String identityName) {
        this.identityName = identityName;
    }

    public String getSpirit() {
        return spirit;
    }

    public void setSpirit(String spirit) {
        this.spirit = spirit;
    }

    public String getSpiritName() {
        return spiritName;
    }

    public void setSpiritName(String spiritName) {
        this.spiritName = spiritName;
    }

    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public String getRelationLabel() {
        return relationLabel;
    }

    public void setRelationLabel(String relationLabel) {
        this.relationLabel = relationLabel;
    }
}
