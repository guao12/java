package com.fenglin.springboottest.service;

import com.fenglin.springboottest.vo.CaptchaVO;

/**
 * 图形验证码：用于阻断注册验证码被脚本批量刷取。
 * 令牌由服务端签发（不可伪造），答案仅存于服务端内存，校验通过即销毁（防重放）。
 *
 * @author fenglin
 */
public interface CaptchaService {

    /** 生成一张图形验证码，返回服务端令牌 + base64 图片 */
    CaptchaVO generate();

    /**
     * 校验图形验证码。
     *
     * @return 校验通过返回 true，并消费该令牌（后续不可复用）；其它情况返回 false
     */
    boolean verify(String token, String code);
}
