package com.fenglin.springboottest.service.impl;

import com.fenglin.springboottest.service.TokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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

    /** HS256 最小密钥长度（字节） */
    private static final int MIN_SECRET_BYTES = 32;

    /** 仓库 application-dev.properties 里的 dev 兜底密钥前缀：一旦出现在 prod 即视为泄露 */
    private static final String DEV_FALLBACK_SECRET_PREFIX = "qIGgHT5EjUPbrOiRHVSArkF5";

    public JwtTokenService(@Value("${dr.jwt.secret:}") String secret, Environment environment) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "dr.jwt.secret 未配置：请通过环境变量 DR_JWT_SECRET 注入至少 " + MIN_SECRET_BYTES + " 字节的随机密钥");
        }
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        if (raw.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("dr.jwt.secret 太短（" + raw.length + " 字节，HS256 要求至少 "
                    + MIN_SECRET_BYTES + " 字节），请重新生成");
        }
        boolean prod = Arrays.stream(environment.getActiveProfiles()).anyMatch("prod"::equalsIgnoreCase);
        if (secret.startsWith(DEV_FALLBACK_SECRET_PREFIX)) {
            if (prod) {
                throw new IllegalStateException("prod 环境检测到仓库内置的 dev 兜底 JWT 密钥，该值已公开视为泄露："
                        + "请通过环境变量 DR_JWT_SECRET 注入真实密钥后再启动");
            }
            log.warn("JWT 正在使用仓库内置的 dev 兜底密钥，仅限本地开发，切勿用于生产环境");
        }
        this.key = Keys.hmacShaKeyFor(raw);
    }

    @Override
    public String issue(Long userId, String account, boolean remember, String pwdVer) {
        long seconds = expiresIn(remember);
        Date now = new Date();
        return Jwts.builder()
                .subject(account)
                .claim("uid", userId)
                .claim("pwd", pwdVer == null ? "" : pwdVer)
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
    public Long parseUserId(String token, String pwdVer) {
        try {
            Claims claims = parse(token);
            // 后台管理员令牌（scope=admin）不属于学员会话
            if ("admin".equals(claims.get("scope", String.class))) {
                return null;
            }
            // 密码已改（令牌内携带的 pwd 版本与当前不一致）→ 旧令牌作废
            String tokenPwd = claims.get("pwd", String.class);
            if (pwdVer != null && !pwdVer.equals(tokenPwd)) {
                log.debug("密码已修改，令牌失效");
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
    public Long parseUserId(String token) {
        return parseUserId(token, null);
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
