package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 内容举报工单，对应表 dr_report。
 *
 * <p>与 {@link DrThreadAction} 分开存放：举报是带状态的工单，
 * 且需要精确指向「某一层楼」，而 action 的唯一键只到主题粒度。</p>
 *
 * @author fenglin
 */
@TableName("dr_report")
public class DrReport implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 举报对象：主题 */
    public static final String TARGET_THREAD = "thread";
    /** 举报对象：楼层 */
    public static final String TARGET_REPLY = "reply";

    /** 待处理 */
    public static final int STATUS_PENDING = 0;
    /** 已下架（举报成立） */
    public static final int STATUS_TAKEN_DOWN = 1;
    /** 已驳回（举报不成立） */
    public static final int STATUS_REJECTED = 2;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("target_type")
    private String targetType;

    @TableField("target_id")
    private Long targetId;

    @TableField("thread_id")
    private Long threadId;

    @TableField("reporter_id")
    private Long reporterId;

    @TableField("reporter_name")
    private String reporterName;

    /** spam / spoiler / copyright / abuse / other */
    @TableField("reason")
    private String reason;

    @TableField("detail")
    private String detail;

    @TableField("status")
    private Integer status;

    @TableField("handler_id")
    private Long handlerId;

    @TableField("handle_note")
    private String handleNote;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("handle_time")
    private LocalDateTime handleTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public Long getThreadId() {
        return threadId;
    }

    public void setThreadId(Long threadId) {
        this.threadId = threadId;
    }

    public Long getReporterId() {
        return reporterId;
    }

    public void setReporterId(Long reporterId) {
        this.reporterId = reporterId;
    }

    public String getReporterName() {
        return reporterName;
    }

    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getHandlerId() {
        return handlerId;
    }

    public void setHandlerId(Long handlerId) {
        this.handlerId = handlerId;
    }

    public String getHandleNote() {
        return handleNote;
    }

    public void setHandleNote(String handleNote) {
        this.handleNote = handleNote;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getHandleTime() {
        return handleTime;
    }

    public void setHandleTime(LocalDateTime handleTime) {
        this.handleTime = handleTime;
    }
}
