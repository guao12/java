package com.fenglin.springboottest.vo;

import java.io.Serializable;

/**
 * 后台「主题管理」列表项。
 *
 * <p>比前台 {@link ThreadVO} 多带两样东西，都是为了少点几次：</p>
 * <ul>
 *   <li>{@code content} 完整正文 —— 编辑弹窗要直接回填，不必再发一次详情请求；</li>
 *   <li>{@code status / statusLabel} —— 待审（0）和已下线（-1）的帖子在前台看不见，
 *       后台必须能一眼分辨，否则会以为「列表里少了东西」。</li>
 * </ul>
 *
 * <p>这里不做密级过滤：管理员管理的是全站内容，能不能看见不该由读者身份决定。</p>
 *
 * @author fenglin
 */
public class AdminThreadVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long boardId;
    private String boardCode;
    private String boardName;

    private String title;

    /** 完整正文（编辑回填用） */
    private String content;

    /** 列表摘要（正文前 80 字，换行压成空格） */
    private String excerpt;

    private Long authorId;
    private String authorName;

    /** 1 公开 2 内部 3 机密 4 绝密 */
    private Integer securityLevel;
    private String securityLabel;

    private boolean replyVisible;
    private boolean pinned;
    private boolean essence;

    private Integer viewCount;
    private Integer replyCount;
    private Integer likeCount;
    private Integer heat;

    /** 1 正常 / 0 待审 / -1 已下线 */
    private Integer status;
    private String statusLabel;

    private String createdAt;
    private String updatedAt;

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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
