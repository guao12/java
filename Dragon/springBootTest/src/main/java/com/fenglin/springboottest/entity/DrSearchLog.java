package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 检索日志，对应表 dr_search_log。用于热词统计与检索审计。
 *
 * @author fenglin
 */
@TableName("dr_search_log")
public class DrSearchLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 检索人 ID，未登录为 NULL */
    @TableField("user_id")
    private Long userId;

    @TableField("keyword")
    private String keyword;

    @TableField("top_category")
    private Long topCategory;

    @TableField("category_id")
    private Long categoryId;

    @TableField("level")
    private Integer level;

    @TableField("result_count")
    private Integer resultCount;

    @TableField("client_ip")
    private String clientIp;

    @TableField("create_time")
    private LocalDateTime createTime;

    public DrSearchLog() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Long getTopCategory() {
        return topCategory;
    }

    public void setTopCategory(Long topCategory) {
        this.topCategory = topCategory;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Integer getResultCount() {
        return resultCount;
    }

    public void setResultCount(Integer resultCount) {
        this.resultCount = resultCount;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
