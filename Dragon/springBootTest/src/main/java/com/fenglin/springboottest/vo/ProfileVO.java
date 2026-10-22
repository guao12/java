package com.fenglin.springboottest.vo;

import com.fenglin.springboottest.entity.DrUser;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 身份卡（用户档案）视图对象 —— 只暴露安全字段，坚决不带 password。
 * 供 /api/user/profile、登录返回的 user 字段共用。
 *
 * @author fenglin
 */
public class ProfileVO {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Long id;
    private String account;
    private String codename;
    private String identity;
    private String spirit;
    private String identityName;
    private String spiritName;
    private Integer loginCount;
    private String lastLoginAt;
    private String createdAt;

    public ProfileVO() {
    }

    /** 从实体转换，identity/spirit 的中文名由字典服务填充 */
    public static ProfileVO from(DrUser user, String identityName, String spiritName) {
        if (user == null) {
            return null;
        }
        ProfileVO vo = new ProfileVO();
        vo.setId(user.getId());
        vo.setAccount(user.getAccount());
        vo.setCodename(user.getCodename());
        vo.setIdentity(user.getIdentity());
        vo.setSpirit(user.getSpirit());
        vo.setIdentityName(identityName);
        vo.setSpiritName(spiritName);
        vo.setLoginCount(user.getLoginCount() == null ? 0 : user.getLoginCount());
        vo.setLastLoginAt(user.getLastLoginAt() == null ? null : FMT.format(user.getLastLoginAt()));
        vo.setCreatedAt(user.getCreateTime() == null ? null : FMT.format(user.getCreateTime()));
        return vo;
    }

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

    public String getSpirit() {
        return spirit;
    }

    public void setSpirit(String spirit) {
        this.spirit = spirit;
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

    public Integer getLoginCount() {
        return loginCount;
    }

    public void setLoginCount(Integer loginCount) {
        this.loginCount = loginCount;
    }

    public String getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(String lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
