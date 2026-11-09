package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 论坛版块实体，对应表 dr_board。
 *
 * <p>版块可见性只看在校身份，判定逻辑见 {@code ForumServiceImpl#canReadBoard}：</p>
 * <pre>
 *   min_identity  最低在校身份（guest &lt; student &lt; executor &lt; teacher）
 * </pre>
 *
 * <p>表里仍保留 {@code min_points} 列（历史遗留，任务系统下线后不再参与判定）。</p>
 *
 * <p>发帖权限另算：{@code post_min_identity}，公告版（read_only=1）只允许教员发。</p>
 *
 * @author fenglin
 */
@TableName("dr_board")
public class DrBoard implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 版块标识，如 canon / fanwork，前端路由与接口都用它 */
    @TableField("code")
    private String code;

    /** 版块名称 */
    @TableField("name")
    private String name;

    /** 英文副标题（装饰用） */
    @TableField("subtitle")
    private String subtitle;

    /** 版块简介 */
    @TableField("description")
    private String description;

    /** 图标名，对应前端 AppIcon 的 name */
    @TableField("icon")
    private String icon;

    /** 排序，越小越前 */
    @TableField("sort_no")
    private Integer sortNo;

    /** 可见所需最低身份 guest/student/executor/teacher */
    @TableField("min_identity")
    private String minIdentity;

    /** 发帖所需最低身份 */
    @TableField("post_min_identity")
    private String postMinIdentity;

    /** 1=只读（公告版），仅教员可发 */
    @TableField("read_only")
    private Integer readOnly;

    /** 1=开放 0=关闭 */
    @TableField("status")
    private Integer status;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    public DrBoard() {
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

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }

    public String getMinIdentity() {
        return minIdentity;
    }

    public void setMinIdentity(String minIdentity) {
        this.minIdentity = minIdentity;
    }

    public String getPostMinIdentity() {
        return postMinIdentity;
    }

    public void setPostMinIdentity(String postMinIdentity) {
        this.postMinIdentity = postMinIdentity;
    }

    public Integer getReadOnly() {
        return readOnly;
    }

    public void setReadOnly(Integer readOnly) {
        this.readOnly = readOnly;
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
