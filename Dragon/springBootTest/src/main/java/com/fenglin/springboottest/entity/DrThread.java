package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 论坛主题实体，对应表 dr_thread。
 *
 * <p>几个冗余字段是刻意保留的：</p>
 * <ul>
 *   <li>{@code authorName / authorIdentity}：发帖时的代号快照。用户之后改代号，
 *       历史帖子里的署名不应该跟着变，否则「谁说的」会被悄悄改写。</li>
 *   <li>{@code replyCount / lastReplyAt / heat}：列表排序直接读，不必每次 COUNT。
 *       由 {@code ForumServiceImpl} 在同一事务里维护。</li>
 * </ul>
 *
 * <p>{@code securityLevel} 与档案共用一套判定：1 公开 2 内部 3 机密 4 绝密，
 * 可见上限由在校身份决定（{@code SecurityLevel#maxLevelOf}）。</p>
 *
 * @author fenglin
 */
@TableName("dr_thread")
public class DrThread implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("board_id")
    private Long boardId;

    /** 发帖人 dr_user.id；0 = 馆藏旧帖（种子数据） */
    @TableField("author_id")
    private Long authorId;

    @TableField("author_name")
    private String authorName;

    @TableField("author_identity")
    private String authorIdentity;

    @TableField("title")
    private String title;

    /** 正文，纯文本；返回给前端时由前端按段落渲染，绝不当 HTML 用 */
    @TableField("content")
    private String content;

    /** 1=公开 2=内部 3=机密 4=绝密 */
    @TableField("security_level")
    private Integer securityLevel;

    /** 1=回复后可见正文 */
    @TableField("reply_visible")
    private Integer replyVisible;

    /** 1=镇馆（置顶） */
    @TableField("pinned")
    private Integer pinned;

    /** 1=入藏（精华） */
    @TableField("essence")
    private Integer essence;

    @TableField("view_count")
    private Integer viewCount;

    @TableField("reply_count")
    private Integer replyCount;

    @TableField("like_count")
    private Integer likeCount;

    /** 热度 = 浏览 + 批注*3 + 点赞*5，写操作时重算 */
    @TableField("heat")
    private Integer heat;

    /** 1=正常 0=待审 -1=已下线 */
    @TableField("status")
    private Integer status;

    @TableField("last_reply_at")
    private LocalDateTime lastReplyAt;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    public DrThread() {
    }

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

    public Integer getSecurityLevel() {
        return securityLevel;
    }

    public void setSecurityLevel(Integer securityLevel) {
        this.securityLevel = securityLevel;
    }

    public Integer getReplyVisible() {
        return replyVisible;
    }

    public void setReplyVisible(Integer replyVisible) {
        this.replyVisible = replyVisible;
    }

    public Integer getPinned() {
        return pinned;
    }

    public void setPinned(Integer pinned) {
        this.pinned = pinned;
    }

    public Integer getEssence() {
        return essence;
    }

    public void setEssence(Integer essence) {
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

    public LocalDateTime getLastReplyAt() {
        return lastReplyAt;
    }

    public void setLastReplyAt(LocalDateTime lastReplyAt) {
        this.lastReplyAt = lastReplyAt;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
