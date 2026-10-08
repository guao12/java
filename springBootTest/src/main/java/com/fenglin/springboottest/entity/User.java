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
 * 骑士（用户）实体，对应表 kr_user。
 *
 * <p>MyBatis-Plus 约定：表名/字段名的驼峰策略已在 application.properties 中全局开启，
 * 这里仍然显式写 @TableName / @TableField，方便对照数据库表结构阅读。</p>
 *
 * @author fenglin
 */
@TableName("kr_user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键，数据库自增 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 骑士名 / 登录账号，唯一 */
    @TableField("username")
    private String username;

    /** 召集令邮箱，唯一 */
    @TableField("email")
    private String email;

    /** BCrypt 加密后的密码，绝不返回给前端 */
    @TableField("password")
    private String password;

    /** 当前使用的形态（rabbittank / hazard / genius） */
    @TableField("form_key")
    private String formKey;

    /** 状态：1=正常 0=禁用 */
    @TableField("status")
    private Integer status;

    /** 逻辑删除标记：0=未删除 1=已删除（@TableLogic 让 MyBatis-Plus 自动拼条件） */
    @TableLogic
    @TableField(value = "deleted", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Integer deleted;

    /** 创建时间，由数据库 CURRENT_TIMESTAMP 生成，Java 侧只读 */
    @TableField(value = "create_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;

    /** 更新时间，由数据库 ON UPDATE CURRENT_TIMESTAMP 维护，Java 侧只读 */
    @TableField(value = "update_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updateTime;

    public User() {
    }

    public User(String username, String email, String password, String formKey) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.formKey = formKey;
        this.status = 1;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFormKey() {
        return formKey;
    }

    public void setFormKey(String formKey) {
        this.formKey = formKey;
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
        return "User{id=" + id + ", username='" + username + "', email='" + email + "'}";
    }
}
