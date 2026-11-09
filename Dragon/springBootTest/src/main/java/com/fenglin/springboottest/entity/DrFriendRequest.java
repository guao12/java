package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 好友申请实体，对应表 dr_friend_request。
 *
 * <p>唯一键 {@code (from_user_id, to_user_id)} 保证同一方向只有一条记录：
 * 被拒之后可以再申请，走的是 UPDATE 把 status 置回 0，而不是插入新行 ——
 * 这样「待处理」列表永远不会出现同一个人的多条申请。</p>
 *
 * @author fenglin
 */
@TableName("dr_friend_request")
public class DrFriendRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 待处理 */
    public static final int STATUS_PENDING = 0;
    /** 已同意 */
    public static final int STATUS_ACCEPTED = 1;
    /** 已拒绝 */
    public static final int STATUS_REJECTED = 2;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 申请人 dr_user.id */
    @TableField("from_user_id")
    private Long fromUserId;

    /** 被申请人 dr_user.id */
    @TableField("to_user_id")
    private Long toUserId;

    /** 申请附言 */
    @TableField("message")
    private String message;

    /** 0=待处理 1=已同意 2=已拒绝 */
    @TableField("status")
    private Integer status;

    /** 处理时间 */
    @TableField("handled_at")
    private LocalDateTime handledAt;

    /** 申请时间，由数据库生成 */
    @TableField(value = "create_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;

    /** 更新时间，由数据库维护 */
    @TableField(value = "update_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updateTime;

    public DrFriendRequest() {
    }

    public DrFriendRequest(Long fromUserId, Long toUserId, String message) {
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.message = message;
        this.status = STATUS_PENDING;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(Long fromUserId) {
        this.fromUserId = fromUserId;
    }

    public Long getToUserId() {
        return toUserId;
    }

    public void setToUserId(Long toUserId) {
        this.toUserId = toUserId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getHandledAt() {
        return handledAt;
    }

    public void setHandledAt(LocalDateTime handledAt) {
        this.handledAt = handledAt;
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
