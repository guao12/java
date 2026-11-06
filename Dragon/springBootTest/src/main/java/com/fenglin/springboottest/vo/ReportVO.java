package com.fenglin.springboottest.vo;

import java.io.Serializable;

/**
 * 举报工单视图（后台队列用）。
 *
 * <p>刻意带上被举报内容的快照（标题 / 楼层号 / 摘要）：处理人要在不跳页的情况下
 * 判断该不该下架，把上下文塞进列表比让管理员一条条点开更实际。
 * 内容被删掉之后，快照依然能说明「当时举报的是什么」。</p>
 *
 * @author fenglin
 */
public class ReportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** thread / reply */
    private String targetType;

    private Long targetId;

    private Long threadId;

    /** 主题标题 */
    private String threadTitle;

    /** 楼层号（targetType=reply 时有值） */
    private Integer floorNo;

    /** 被举报内容摘要 */
    private String targetExcerpt;

    /** 被举报内容作者 */
    private String targetAuthor;

    private Long reporterId;

    private String reporterName;

    /** spam / spoiler / copyright / abuse / other */
    private String reason;

    /** 原因的中文label */
    private String reasonLabel;

    private String detail;

    /** 0 待处理 / 1 已下架 / 2 已驳回 */
    private Integer status;

    private String statusLabel;

    private String createTime;

    private String handleTime;

    private String handleNote;

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

    public String getThreadTitle() {
        return threadTitle;
    }

    public void setThreadTitle(String threadTitle) {
        this.threadTitle = threadTitle;
    }

    public Integer getFloorNo() {
        return floorNo;
    }

    public void setFloorNo(Integer floorNo) {
        this.floorNo = floorNo;
    }

    public String getTargetExcerpt() {
        return targetExcerpt;
    }

    public void setTargetExcerpt(String targetExcerpt) {
        this.targetExcerpt = targetExcerpt;
    }

    public String getTargetAuthor() {
        return targetAuthor;
    }

    public void setTargetAuthor(String targetAuthor) {
        this.targetAuthor = targetAuthor;
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

    public String getReasonLabel() {
        return reasonLabel;
    }

    public void setReasonLabel(String reasonLabel) {
        this.reasonLabel = reasonLabel;
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

    public String getStatusLabel() {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getHandleTime() {
        return handleTime;
    }

    public void setHandleTime(String handleTime) {
        this.handleTime = handleTime;
    }

    public String getHandleNote() {
        return handleNote;
    }

    public void setHandleNote(String handleNote) {
        this.handleNote = handleNote;
    }
}
