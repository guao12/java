package com.fenglin.springboottest.vo;

import java.io.Serializable;

/**
 * 主题列表项。
 *
 * <p>{@code locked = true} 表示密级不足：标题仍然返回（让人知道有这么个讨论），
 * 但正文不会出现在任何列表接口里 —— 详情接口会给出「封存」说明。</p>
 *
 * @author fenglin
 */
public class ThreadVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long boardId;
    private String boardCode;
    private String boardName;

    private String title;

    /** 摘要（正文首段截断，已按密级判定决定是否给） */
    private String excerpt;

    private Long authorId;
    private String authorName;
    private String authorIdentity;

    /** 1 公开 2 内部 3 机密 4 绝密 */
    private Integer securityLevel;
    private String securityLabel;

    /** 1=回复后可见 */
    private boolean replyVisible;
    private boolean pinned;
    private boolean essence;

    private Integer viewCount;
    private Integer replyCount;
    private Integer likeCount;
    private Integer heat;

    private String createdAt;
    private String lastReplyAt;

    /** 密级不足 */
    private boolean locked;

    /** 是否本人发布 */
    private boolean mine;

    /** 当前访问者是否已点赞 */
    private boolean liked;

    /** 当前访问者是否已收藏 */
    private boolean collected;

    /**
     * 是否被编辑过。
     *
     * <p>用 update_time > create_time 判定，不额外加列：
     * 浏览量 / 楼层数 / 点赞数的自增都是手写 SQL，不会碰 update_time，
     * 所以这个比较只会在「真的改了标题或正文」时成立。</p>
     */
    private boolean edited;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBoardId() {
        return boardId;
    }

    public void setBoardId(Long boardId) {
        this.boardId = boardId;
    }

    public String getBoardCode() {
        return boardCode;
    }

    public void setBoardCode(String boardCode) {
        this.boardCode = boardCode;
    }

    public String getBoardName() {
        return boardName;
    }

    public void setBoardName(String boardName) {
        this.boardName = boardName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getExcerpt() {
        return excerpt;
    }

    public void setExcerpt(String excerpt) {
        this.excerpt = excerpt;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorIdentity() {
        return authorIdentity;
    }

    public void setAuthorIdentity(String authorIdentity) {
        this.authorIdentity = authorIdentity;
    }

    public Integer getSecurityLevel() {
        return securityLevel;
    }

    public void setSecurityLevel(Integer securityLevel) {
        this.securityLevel = securityLevel;
    }

    public String getSecurityLabel() {
        return securityLabel;
    }

    public void setSecurityLabel(String securityLabel) {
        this.securityLabel = securityLabel;
    }

    public boolean isReplyVisible() {
        return replyVisible;
    }

    public void setReplyVisible(boolean replyVisible) {
        this.replyVisible = replyVisible;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public boolean isEssence() {
        return essence;
    }

    public void setEssence(boolean essence) {
        this.essence = essence;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public Integer getReplyCount() {
        return replyCount;
    }

    public void setReplyCount(Integer replyCount) {
        this.replyCount = replyCount;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public Integer getHeat() {
        return heat;
    }

    public void setHeat(Integer heat) {
        this.heat = heat;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getLastReplyAt() {
        return lastReplyAt;
    }

    public void setLastReplyAt(String lastReplyAt) {
        this.lastReplyAt = lastReplyAt;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public boolean isMine() {
        return mine;
    }

    public void setMine(boolean mine) {
        this.mine = mine;
    }

    public boolean isLiked() {
        return liked;
    }

    public void setLiked(boolean liked) {
        this.liked = liked;
    }

    public boolean isEdited() {
        return edited;
    }

    public void setEdited(boolean edited) {
        this.edited = edited;
    }

    public boolean isCollected() {
        return collected;
    }

    public void setCollected(boolean collected) {
        this.collected = collected;
    }
}
