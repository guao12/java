package com.fenglin.springboottest.vo;

/**
 * 图形验证码返回：服务端签发的不可伪造令牌 + base64 PNG 图片。
 * 前端展示图片，用户照填文字，提交时连同 token 一起回传，由后端校验并消费。
 *
 * @author fenglin
 */
public class CaptchaVO {

    /** 服务端签发的随机令牌，与正确答案一一对应，存在内存中、5 分钟过期 */
    private String token;

    /** base64 PNG 图片（data:image/png;base64,...），直接用于 <img :src> */
    private String image;

    public CaptchaVO() {
    }

    public CaptchaVO(String token, String image) {
        this.token = token;
        this.image = image;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
