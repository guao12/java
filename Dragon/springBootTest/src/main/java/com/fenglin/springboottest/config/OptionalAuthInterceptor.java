package com.fenglin.springboottest.config;

import com.fenglin.springboottest.common.CurrentUser;
import com.fenglin.springboottest.entity.DrUser;
import com.fenglin.springboottest.service.AuthService;
import com.fenglin.springboottest.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 可选鉴权拦截器：能识别登录者身份，但不拦截未登录请求。
 *
 * <p>首页与检索接口对所有人开放，但不同身份的人能看到不同密级的卷宗
 * （见 {@code SecurityLevel#maxLevelOf}）。本拦截器负责：
 * 「带令牌 → 解析成功 → 写入 {@link CurrentUser}」；
 * 「不带令牌 / 令牌失效 → 什么也不做，接口按游客的公开级权限返回」。</p>
 *
 * <p>与 {@link JwtInterceptor} 的区别：那个是强制鉴权（/api/user/**），这个是软识别。</p>
 *
 * @author fenglin
 */
@Component
public class OptionalAuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenService tokenService;
    private final AuthService authService;

    public OptionalAuthInterceptor(TokenService tokenService, AuthService authService) {
        this.tokenService = tokenService;
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            String header = request.getHeader("Authorization");
            if (header == null || !header.startsWith(BEARER_PREFIX)) {
                return true;
            }
            String token = header.substring(BEARER_PREFIX.length()).trim();
            Long userId = tokenService.parseUserId(token);
            if (userId == null) {
                return true;
            }
            DrUser user = authService.getUserById(userId);
            if (user != null) {
                CurrentUser.set(user);
            }
        } catch (Exception e) {
            // 软鉴权：任何异常都降级为「未登录」，绝不影响接口正常返回
            CurrentUser.clear();
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 必须清理，防止线程复用导致串号
        CurrentUser.clear();
    }
}
