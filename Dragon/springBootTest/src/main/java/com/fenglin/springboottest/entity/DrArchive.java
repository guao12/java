package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 档案实体，对应表 dr_archive。
 *
 * <p>核心字段 security_level（密级）决定谁能看到这条卷宗，
 * 可见上限由当前登录者的在校身份决定，见 {@code SearchServiceImpl#maxLevel()}。</p>
 *
 * @author fenglin
 */
@TableName("dr_archive")
public class DrArchive implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 卷宗编号，唯一，如 AR-P-001 */
    @TableField("code")
    private String code;

    /** 标题 */
    @TableField("title")
    private String title;

    /** 摘要 */
    @TableField("summary")
    private String summary;

    /** 正文，仅详情接口返回 */
    @TableField("content")
    private String content;

    /** 一级分类 ID（冗余字段，便于按大类筛选） */
    @TableField("top_category_id")
    private Long topCategoryId;

    /** 二级分类 ID */
    @TableField("category_id")
    private Long categoryId;

    /** 密级：1=公开 2=内部 3=机密 4=绝密 */
    @TableField("security_level")
    private Integer securityLevel;

    /** 评级标签：S / A / B / 龙王 / 禁咒 / 禁区… */
    @TableField("rank_label")
    private String rankLabel;

    /** 标签，英文逗号分隔 */
    @TableField("tags")
    private String tags;

    /** 来源 / 归档部门 */
    @TableField("source")
    private String source;

    /** 浏览量 */
    @TableField("view_count")
    private Integer viewCount;

    /** 热度 */
    @TableField("heat")
    private Integer heat;

    /** 首页精选：1=精选 0=普通 */
    @TableField("spotlight")
    private Integer spotlight;

    /** 1=公开上架 0=封存 */
    @TableField("status")
    private Integer status;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    public DrArchive() {
    }

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getTopCategoryId() {
        return topCategoryId;
    }

    public void setTopCategoryId(Long topCategoryId) {
        this.topCategoryId = topCategoryId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Integer getSecurityLevel() {
        return securityLevel;
    }

    public void setSecurityLevel(Integer securityLevel) {
        this.securityLevel = securityLevel;
    }

    public String getRankLabel() {
        return rankLabel;
    }

    public void setRankLabel(String rankLabel) {
        this.rankLabel = rankLabel;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public Integer getHeat() {
        return heat;
    }

    public void setHeat(Integer heat) {
        this.heat = heat;
    }

    public Integer getSpotlight() {
        return spotlight;
    }

    public void setSpotlight(Integer spotlight) {
        this.spotlight = spotlight;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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
