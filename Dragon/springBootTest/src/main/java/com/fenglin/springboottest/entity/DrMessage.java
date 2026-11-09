package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 私信实体，对应表 dr_message。
 *
 * <p>一对一私聊不建会话表：会话就是 {@code (sender_id, receiver_id)} 的组合，
 * 会话列表用相关子查询取最后一条即可。数据量在千级时比维护会话表省事，
 * 也不会出现「会话表与消息表对不上」的一致性麻烦。</p>
 *
 * @author fenglin
 */
@TableName("dr_message")
public class DrMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int UNREAD = 0;
    public static final int READ = 1;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 发送者 dr_user.id */
    @TableField("sender_id")
    private Long senderId;

    /** 接收者 dr_user.id */
    @TableField("receiver_id")
    private Long receiverId;

    /** 内容（纯文本） */
    @TableField("content")
    private String content;

    /** 0=未读 1=已读 */
    @TableField("read_flag")
    private Integer readFlag;

    /** 发送时间，由数据库生成 */
    @TableField(value = "create_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;

    public DrMessage() {
    }

    public DrMessage(Long senderId, Long receiverId, String content) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.readFlag = UNREAD;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getReadFlag() {
        return readFlag;
    }

    public void setReadFlag(Integer readFlag) {
        this.readFlag = readFlag;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
