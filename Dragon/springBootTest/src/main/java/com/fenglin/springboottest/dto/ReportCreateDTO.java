package com.fenglin.springboottest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * 举报提交参数。
 *
 * <p>targetType / targetId 由前端从上下文带上来（主楼 = thread，楼层 = reply）。
 * threadId 单独给：举报楼层时，后台要能顺着它把整帖上下文捞出来判断。</p>
 *
 * @author fenglin
 */
public class ReportCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** thread / reply */
    @NotBlank(message = "举报对象类型不能为空")
    private String targetType;

    /** 被举报对象 id */
    private Long targetId;

    /** 所属主题 id */
    private Long threadId;

    /** spam / spoiler / copyright / abuse / other */
    @NotBlank(message = "请选择举报原因")
    private String reason;

    /** 补充说明，选填 */
    @Size(max = 500, message = "补充说明最多 500 字")
    private String detail;

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
}
