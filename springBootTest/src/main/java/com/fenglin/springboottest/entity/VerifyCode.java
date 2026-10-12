package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 注册邮箱验证码。
 *
 * <p>注册时先调 send-code 为邮箱下发一个 6 位数字验证码（5 分钟有效），
 * 注册提交时带上 code，后端校验通过后作废。</p>
 *
 * @author fenglin
 */
@TableName("kr_verify_code")
public class VerifyCode {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 邮箱 */
    private String email;

    /** 6 位数字验证码 */
    private String code;

    /** 过期时间（创建时间 + 5 分钟） */
    private LocalDateTime expireTime;

    /** 0=未使用 1=已使用 */
    private Integer used;

    /** 创建时间 */
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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
