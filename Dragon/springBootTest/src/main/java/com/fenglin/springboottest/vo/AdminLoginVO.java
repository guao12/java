package com.fenglin.springboottest.vo;

import java.io.Serializable;

/**
 * 后台管理员登录结果：JWT + 管理员基本信息。
 *
 * @author fenglin
 */
public class AdminLoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 后台 JWT，请求时放 Authorization: Bearer &lt;token&gt; */
    private String token;

    /** 令牌有效期（秒） */
    private Long expiresIn;

    /** 管理员信息（不含密码） */
    private AdminInfo admin;

    public AdminLoginVO() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public AdminInfo getAdmin() {
        return admin;
    }

    public void setAdmin(AdminInfo admin) {
        this.admin = admin;
    }

    /** 管理员基本信息 */
    public static class AdminInfo implements Serializable {

        private static final long serialVersionUID = 1L;

        private Long id;
        private String username;
        private String nickname;

        public AdminInfo() {
        }

        public AdminInfo(Long id, String username, String nickname) {
            this.id = id;
            this.username = username;
            this.nickname = nickname;
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

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }
    }
}
