package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 混血种（用户）实体，对应表 dr_user。
 *
 * <p>MyBatis-Plus 约定：驼峰转下划线已在 application.properties 全局开启，
 * 这里仍然显式写 @TableName / @TableField，方便对照数据库表结构阅读。</p>
 *
 * @author fenglin
 */
@TableName("dr_user")
public class DrUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键，数据库自增 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 学院编号（学号），如 CC20260001，唯一，注册时由后端签发 */
    @TableField("account")
    private String account;

    /** 姓名 / 行动代号 */
    @TableField("codename")
    private String codename;

    /** BCrypt 加密后的言灵密钥，绝不返回给前端 */
    @TableField("password")
    private String password;

    /** 在校身份 code：freshman/student/executor/teacher/guest */
    @TableField("identity")
    private String identity;

    /** 言灵 code：none/junyan/bronze/shiling/fengwang/zhulong/wangquan */
    @TableField("spirit")
    private String spirit;

    /** 状态：1=在册 0=封印（禁用） */
    @TableField("status")
    private Integer status;

    /** 逻辑删除标记：0=未删除 1=已删除（@TableLogic 让 MyBatis-Plus 自动拼条件） */
    @TableLogic
    @TableField(value = "deleted", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Integer deleted;

    /** 累计登录次数 */
    @TableField("login_count")
    private Integer loginCount;

    /** 最近登录时间 */
    @TableField("last_login_at")
    private LocalDateTime lastLoginAt;

    /** 登记时间，由数据库 CURRENT_TIMESTAMP 生成，Java 侧只读 */
    @TableField(value = "create_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;

    /** 更新时间，由数据库 ON UPDATE CURRENT_TIMESTAMP 维护，Java 侧只读 */
    @TableField(value = "update_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updateTime;

    public DrUser() {
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

    public String getCodename() {
        return codename;
    }

    public void setCodename(String codename) {
        this.codename = codename;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public String getSpirit() {
        return spirit;
    }

    public void setSpirit(String spirit) {
        this.spirit = spirit;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public Integer getLoginCount() {
        return loginCount;
    }

    public void setLoginCount(Integer loginCount) {
        this.loginCount = loginCount;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
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
        // 注意：日志里绝不输出 password
        return "DrUser{id=" + id + ", account='" + account + "', codename='" + codename + "'}";
    }
}
