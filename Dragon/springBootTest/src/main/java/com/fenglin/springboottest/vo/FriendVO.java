package com.fenglin.springboottest.vo;

import java.io.Serializable;

/**
 * 好友列表项（也用于概览里的「最近会话」）。
 *
 * <p>时间字段统一用字符串：Mapper 里已用 DATE_FORMAT 格式化成
 * {@code yyyy-MM-ddTHH:mm:ss}，前端 new Date(...) 各浏览器都能正确解析，
 * 也避免本地时区在序列化环节被擅自加后缀。</p>
 *
 * @author fenglin
 */
public class FriendVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 好友 dr_user.id */
    private Long id;

    private String account;
    private String codename;

    /** 在校身份 code / 中文名 */
    private String identity;
    private String identityName;

    /** 言灵 code / 中文名 */
    private String spirit;
    private String spiritName;

    /** 成为好友的时间 */
    private String since;

    /** 最后一条消息内容，没聊过为 null */
    private String lastMessage;

    /** 最后一条消息时间 */
    private String lastAt;

    /** 我这边未读的条数 */
    private Integer unread;

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

    public String getSince() {
        return since;
    }

    public void setSince(String since) {
        this.since = since;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getLastAt() {
        return lastAt;
    }

    public void setLastAt(String lastAt) {
        this.lastAt = lastAt;
    }

    public Integer getUnread() {
        return unread;
    }

    public void setUnread(Integer unread) {
        this.unread = unread;
    }
}
