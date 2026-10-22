package com.fenglin.springboottest.service;

/**
 * JWT 令牌服务：签发 / 解析。
 * 无状态鉴权：令牌自包含，服务端不存 Session。
 *
 * @author fenglin
 */
public interface TokenService {

    /**
     * 为指定用户签发令牌。
     *
     * @param userId    用户主键
     * @param account   学院编号（学号），作为 subject
     * @param remember true = 长令牌（默认 7 天），false = 短令牌（默认 12 小时）
     * @return JWT 字符串
     */
    String issue(Long userId, String account, boolean remember);

    /**
     * 令牌有效期（秒）。
     */
    long expiresIn(boolean remember);

    /**
     * 解析并校验令牌。
     *
     * @param token JWT 字符串
     * @return 用户主键；令牌非法 / 过期返回 null
     */
    Long parseUserId(String token);
}
