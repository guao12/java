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

    /**
     * 为后台管理员签发令牌（与用户令牌隔离：带 scope=admin 声明，
     * 学员令牌进不了后台接口，管理员令牌也进不了学员接口）。
     *
     * @param adminId  管理员主键
     * @param username 管理员账号
     * @param remember true = 长令牌，false = 短令牌
     * @return JWT 字符串
     */
    String issueAdmin(Long adminId, String username, boolean remember);

    /**
     * 解析并校验后台管理员令牌。
     *
     * @param token JWT 字符串
     * @return 管理员主键；非管理员令牌 / 非法 / 过期返回 null
     */
    Long parseAdminId(String token);
}
