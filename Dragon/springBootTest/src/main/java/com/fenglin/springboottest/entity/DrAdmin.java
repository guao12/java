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
 * 后台管理员实体，对应表 dr_admin。
 *
 * <p>后台管理系统只有登录入口、没有注册入口，账号由本 SQL 脚本预置，
 * 如需新增管理员请手工执行 INSERT（密码用 BCrypt 加密）。</p>
 *
 * @author fenglin
 */
@TableName("dr_admin")
public class DrAdmin implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键，数据库自增 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 登录账号 */
    @TableField("username")
    private String username;

    /** BCrypt 加密后的密码，绝不返回给前端 */
    @TableField("password")
    private String password;

    /** 显示名 */
    @TableField("nickname")
    private String nickname;

    /** 状态：1=在册 0=封印（禁用） */
    @TableField("status")
    private Integer status;

    /** 逻辑删除标记：0=未删除 1=已删除 */
    @TableLogic
    @TableField(value = "deleted", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private Integer deleted;

    /** 累计登录次数 */
    @TableField("login_count")
    private Integer loginCount;

    /** 最近登录时间 */
    @TableField("last_login_at")
    private LocalDateTime lastLoginAt;

    /** 创建时间，数据库生成，Java 侧只读 */
    @TableField(value = "create_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;

    /** 更新时间，数据库维护，Java 侧只读 */
    @TableField(value = "update_time", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updateTime;

    public DrAdmin() {
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
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
        // 日志里绝不输出 password
        return "DrAdmin{id=" + id + ", username='" + username + "', nickname='" + nickname + "'}";
    }
}
