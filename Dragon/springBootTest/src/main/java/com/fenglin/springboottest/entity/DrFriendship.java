package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 好友关系实体，对应表 dr_friendship。
 *
 * <p><b>一个方向一行</b>：A 与 B 成为好友时插入 (A→B) 和 (B→A) 两行。
 * 冗余是为了让「我的好友列表」「是否已是好友」都只走单表唯一索引，
 * 不必写 {@code WHERE a=? OR b=?} 这种会让索引失效的条件。
 * 代价是删除好友必须两行同删 —— 统一由 FriendServiceImpl 保证。</p>
 *
 * @author fenglin
 */
@TableName("dr_friendship")
public class DrFriendship implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 归属人 dr_user.id */
    @TableField("user_id")
    private Long userId;

    /** 好友 dr_user.id */
    @TableField("friend_id")
    private Long friendId;

    /** 好友备注（预留字段） */
    @TableField("remark")
    private String remark;

    /** 成为好友的时间，由数据库生成 */
    @TableField(value = "create_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;

    public DrFriendship() {
    }

    public DrFriendship(Long userId, Long friendId) {
        this.userId = userId;
        this.friendId = friendId;
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

    public Long getFriendId() {
        return friendId;
    }

    public void setFriendId(Long friendId) {
        this.friendId = friendId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
