package com.fenglin.springboottest.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fenglin.springboottest.common.CurrentUser;
import com.fenglin.springboottest.common.R;
import com.fenglin.springboottest.entity.DrUser;
import com.fenglin.springboottest.service.AuthService;
import com.fenglin.springboottest.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * JWT 鉴权拦截器：拦截 /api/user/**。
 *
 * <p>校验 Authorization: Bearer &lt;token&gt;，通过后把用户写入 {@link CurrentUser}；
 * 未携带 / 非法 / 过期时返回 HTTP 401 + JSON（前端 http.js 会清除本地会话并跳回登录页）。</p>
 *
 * @author fenglin
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenService tokenService;
    private final AuthService authService;
    private final ObjectMapper objectMapper;

    public JwtInterceptor(TokenService tokenService,
                          AuthService authService,
                          ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return reject(response, "尚未携带身份令牌，请先完成身份认证");
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        Long userId = tokenService.parseUserId(token);
        if (userId == null) {
            return reject(response, "登录状态已失效，请重新认证");
        }
        DrUser user = authService.getUserById(userId);
        if (user == null) {
            return reject(response, "身份卡不存在或已被注销");
        }
        if (user.getStatus() == null || user.getStatus() == 0) {
            return reject(response, "该身份卡已被学院封印，请联系执行部");
        }

        CurrentUser.set(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 一定要清理，防止线程复用导致串号
        CurrentUser.clear();
    }

    /** 写出 HTTP 401 + 统一 JSON 响应体 */
    private boolean reject(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(R.fail(401, message)));
        return false;
    }
}
