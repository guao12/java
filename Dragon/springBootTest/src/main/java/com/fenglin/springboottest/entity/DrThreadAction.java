package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 主题互动实体，对应表 dr_thread_action。
 *
 * <p>点赞 / 收藏 / 举报共用一张表，用 {@code type} 区分，
 * 唯一键 {@code (thread_id, user_id, type)} 保证同一人只能点一次赞。</p>
 *
 * @author fenglin
 */
@TableName("dr_thread_action")
public class DrThreadAction implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String LIKE = "like";
    public static final String COLLECT = "collect";
    public static final String REPORT = "report";

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("thread_id")
    private Long threadId;

    @TableField("user_id")
    private Long userId;

    /** like / collect / report */
    @TableField("type")
    private String type;

    @TableField("create_time")
    private LocalDateTime createTime;

    public DrThreadAction() {
    }

    public DrThreadAction(Long threadId, Long userId, String type) {
        this.threadId = threadId;
        this.userId = userId;
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getThreadId() {
        return threadId;
    }

    public void setThreadId(Long threadId) {
        this.threadId = threadId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
