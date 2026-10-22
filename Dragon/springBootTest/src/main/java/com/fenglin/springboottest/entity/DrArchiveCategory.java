package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 档案分类实体，对应表 dr_archive_category。
 *
 * <p>两级树结构：parent_id = 0 为一级分类，其余为二级分类。
 * 分类 ID 由建表脚本固定分配（1001+ 为一级，2001+ 为二级），便于种子数据引用父子关系。</p>
 *
 * @author fenglin
 */
@TableName("dr_archive_category")
public class DrArchiveCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /** 父级分类 ID，0 = 一级分类 */
    @TableField("parent_id")
    private Long parentId;

    /** 分类 code，全局唯一 */
    @TableField("code")
    private String code;

    /** 分类名称 */
    @TableField("name")
    private String name;

    /** 前端图标名，对应 AppIcon.vue */
    @TableField("icon")
    private String icon;

    /** 排序号 */
    @TableField("sort_no")
    private Integer sortNo;

    /** 1=启用 0=停用 */
    @TableField("status")
    private Integer status;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    public DrArchiveCategory() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
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

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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
