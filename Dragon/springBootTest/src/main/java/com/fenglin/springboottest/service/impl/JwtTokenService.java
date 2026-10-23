package com.fenglin.springboottest.service.impl;

import com.fenglin.springboottest.service.TokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 令牌服务实现（jjwt 0.12.x API，HS256）。
 *
 * <p>密钥在 application.properties 中配置（dr.jwt.secret），
 * HS256 要求密钥长度至少 32 字节。</p>
 *
 * @author fenglin
 */
@Service
public class JwtTokenService implements TokenService {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenService.class);

    private final SecretKey key;

    /** 短令牌有效期（秒），默认 12 小时 */
    @Value("${dr.jwt.expire-seconds:43200}")
    private long expireSeconds;

    /** 长令牌有效期（秒）："记住身份卡" 勾选时生效，默认 7 天 */
    @Value("${dr.jwt.expire-seconds-remember:604800}")
    private long expireSecondsRemember;

    public JwtTokenService(@Value("${dr.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String issue(Long userId, String account, boolean remember) {
        long seconds = expiresIn(remember);
        Date now = new Date();
        return Jwts.builder()
                .subject(account)
                .claim("uid", userId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + seconds * 1000))
                .signWith(key)
                .compact();
    }

    @Override
    public long expiresIn(boolean remember) {
        return remember ? expireSecondsRemember : expireSeconds;
    }

    @Override
    public Long parseUserId(String token) {
        try {
            Claims claims = parse(token);
            // 后台管理员令牌（scope=admin）不属于学员会话
            if ("admin".equals(claims.get("scope", String.class))) {
                return null;
            }
            Number uid = claims.get("uid", Number.class);
            return uid == null ? null : uid.longValue();
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("令牌校验未通过：{}", e.getMessage());
            return null;
        }
    }

    @Override
    public String issueAdmin(Long adminId, String username, boolean remember) {
        long seconds = expiresIn(remember);
        Date now = new Date();
        return Jwts.builder()
                .subject("admin:" + username)
                .claim("aid", adminId)
                .claim("scope", "admin")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + seconds * 1000))
                .signWith(key)
                .compact();
    }

    @Override
    public Long parseAdminId(String token) {
        try {
            Claims claims = parse(token);
            // 只有带 scope=admin 的令牌才算后台会话
            if (!"admin".equals(claims.get("scope", String.class))) {
                return null;
            }
            Number aid = claims.get("aid", Number.class);
            return aid == null ? null : aid.longValue();
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("后台令牌校验未通过：{}", e.getMessage());
            return null;
        }
    }

    /** 解析并验签（HS256） */
    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
