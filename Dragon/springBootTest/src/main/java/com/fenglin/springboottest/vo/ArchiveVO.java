package com.fenglin.springboottest.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 档案卷宗视图对象（列表 / 详情共用）。
 *
 * <p>详情页额外带 content，列表接口不返回正文以减少传输量。</p>
 *
 * @author fenglin
 */
public class ArchiveVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 卷宗编号 */
    private String code;

    /** 标题 */
    private String title;

    /** 摘要 */
    private String summary;

    /** 正文，仅详情接口返回 */
    private String content;

    /** 一级分类 ID */
    private Long topCategoryId;

    /** 一级分类名 */
    private String topCategoryName;

    /** 二级分类 ID */
    private Long categoryId;

    /** 二级分类名 */
    private String categoryName;

    /** 密级：1-4 */
    private Integer securityLevel;

    /** 密级名称：公开 / 内部 / 机密 / 绝密 */
    private String securityLabel;

    /** 评级标签：S / A / B / 龙王 / 禁咒… */
    private String rankLabel;

    /** 标签列表 */
    private List<String> tags;

    /** 来源 / 归档部门 */
    private String source;

    /** 浏览量 */
    private Integer viewCount;

    /** 热度 */
    private Integer heat;

    /** 是否精选 */
    private Boolean spotlight;

    /** 归档时间，yyyy-MM-dd HH:mm:ss */
    private String createTime;

    public ArchiveVO() {
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

    public String getTopCategoryName() {
        return topCategoryName;
    }

    public void setTopCategoryName(String topCategoryName) {
        this.topCategoryName = topCategoryName;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
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

    public String getRankLabel() {
        return rankLabel;
    }

    public void setRankLabel(String rankLabel) {
        this.rankLabel = rankLabel;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
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

    public Boolean getSpotlight() {
        return spotlight;
    }

    public void setSpotlight(Boolean spotlight) {
        this.spotlight = spotlight;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }
}
