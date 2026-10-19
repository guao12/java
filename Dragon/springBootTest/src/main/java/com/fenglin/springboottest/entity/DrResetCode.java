package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 密钥重置验证码实体，对应表 dr_reset_code。
 *
 * <p>验证龙文：6 位数字，5 分钟内有效，同一账号 60 秒冷却，使用后立即作废。</p>
 *
 * @author fenglin
 */
@TableName("dr_reset_code")
public class DrResetCode implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键，数据库自增 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 对应的学院编号（学号） */
    @TableField("account")
    private String account;

    /** 6 位数字验证码 */
    @TableField("code")
    private String code;

    /** 过期时间 */
    @TableField("expire_time")
    private LocalDateTime expireTime;

    /** 0=未使用 1=已使用 */
    @TableField("used")
    private Integer used;

    /** 下发时间，由数据库 CURRENT_TIMESTAMP 生成 */
    @TableField(value = "create_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;

    public DrResetCode() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }

    public Integer getUsed() {
        return used;
    }

    public void setUsed(Integer used) {
        this.used = used;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
