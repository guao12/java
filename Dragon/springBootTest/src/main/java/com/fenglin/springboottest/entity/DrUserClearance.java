package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户权限进度（权限点 / 密级 / 已完成任务），对应表 dr_user_clearance。
 *
 * <p>原先权限点只存在浏览器 localStorage，换电脑即丢失。本表把它挂到
 * {@code dr_user.id} 上，实现「换设备登录后权限自动恢复」。</p>
 *
 * <p>completed / records 是结构化数组，为免新增字段与前端结构频繁耦合，
 * 这里以 JSON 文本整体存取，由 Service 层负责序列化与合并。</p>
 *
 * @author fenglin
 */
@TableName("dr_user_clearance")
public class DrUserClearance implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 关联 dr_user.id，唯一 */
    @TableField("user_id")
    private Long userId;

    /** 冗余学号，便于直接查库排查 */
    @TableField("account")
    private String account;

    /** 累计权限点 */
    @TableField("points")
    private Integer points;

    /** 已完成任务编号数组的 JSON，如 ["EX-001","EX-002"] */
    @TableField("completed_json")
    private String completedJson;

    /** 已完成任务卡快照数组的 JSON（供「已完成」栏回显） */
    @TableField("records_json")
    private String recordsJson;

    @TableField(value = "create_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;

    @TableField(value = "update_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updateTime;

    public DrUserClearance() {
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

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public String getCompletedJson() {
        return completedJson;
    }

    public void setCompletedJson(String completedJson) {
        this.completedJson = completedJson;
    }

    public String getRecordsJson() {
        return recordsJson;
    }

    public void setRecordsJson(String recordsJson) {
        this.recordsJson = recordsJson;
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

    @Override
    public String toString() {
        return "DrUserClearance{userId=" + userId + ", account='" + account + "', points=" + points + '}';
    }
}
