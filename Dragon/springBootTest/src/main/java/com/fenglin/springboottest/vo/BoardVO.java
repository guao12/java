package com.fenglin.springboottest.vo;

import java.io.Serializable;

/**
 * 版块视图（首页卡片 / 版块页头部共用）。
 *
 * <p>{@code locked = true} 表示当前访问者无权进入：卡片仍然展示（让人知道有这个地方），
 * 但带上 {@code lockHint} 说明还差什么 —— 「看不见的版块」不会有人去争取权限。</p>
 *
 * @author fenglin
 */
public class BoardVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String code;
    private String name;
    private String subtitle;
    private String description;
    private String icon;

    /** 可见主题数 */
    private Integer threadCount;

    /** 最后更新时间（ISO，前端做相对时间） */
    private String lastAt;

    /** 当前访问者是否无权进入 */
    private boolean locked;

    /** 无权进入时的说明，如「需执行部身份 · 权限点 8」 */
    private String lockHint;

    /** 进入所需权限点 */
    private Integer minPoints;

    /** 进入所需身份 */
    private String minIdentity;

    /** 1=只读（公告版） */
    private boolean readOnly;

    /** 当前访问者能否在此发帖 */
    private boolean canPost;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Integer getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(Integer threadCount) {
        this.threadCount = threadCount;
    }

    public String getLastAt() {
        return lastAt;
    }

    public void setLastAt(String lastAt) {
        this.lastAt = lastAt;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public String getLockHint() {
        return lockHint;
    }

    public void setLockHint(String lockHint) {
        this.lockHint = lockHint;
    }

    public Integer getMinPoints() {
        return minPoints;
    }

    public void setMinPoints(Integer minPoints) {
        this.minPoints = minPoints;
    }

    public String getMinIdentity() {
        return minIdentity;
    }

    public void setMinIdentity(String minIdentity) {
        this.minIdentity = minIdentity;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public boolean isCanPost() {
        return canPost;
    }

    public void setCanPost(boolean canPost) {
        this.canPost = canPost;
    }
}
