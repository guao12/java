package com.fenglin.springboottest.vo;

/**
 * 登录成功返回：JWT 令牌 + 身份卡档案。
 * 前端 LoginView.vue 保存会话：saveSession({ token, user })。
 *
 * @author fenglin
 */
public class LoginVO {

    /** JWT 令牌，后续请求放在 Authorization: Bearer <token> */
    private String token;

    /** 令牌有效期（秒） */
    private Long expiresIn;

    /** 身份卡档案 */
    private ProfileVO user;

    public LoginVO() {
    }

    public LoginVO(String token, Long expiresIn, ProfileVO user) {
        this.token = token;
        this.expiresIn = expiresIn;
        this.user = user;
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

    public ProfileVO getUser() {
        return user;
    }

    public void setUser(ProfileVO user) {
        this.user = user;
    }
}
