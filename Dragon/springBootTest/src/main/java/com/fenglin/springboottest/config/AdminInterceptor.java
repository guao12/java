package com.fenglin.springboottest.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fenglin.springboottest.common.CurrentAdmin;
import com.fenglin.springboottest.common.R;
import com.fenglin.springboottest.entity.DrAdmin;
import com.fenglin.springboottest.service.AdminService;
import com.fenglin.springboottest.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 后台管理 JWT 拦截器：拦截 /api/admin/**（登录接口除外）。
 *
 * <p>校验 Authorization: Bearer &lt;token&gt;，令牌必须是管理员令牌
 * （scope=admin，见 TokenService#issueAdmin）；通过后把管理员写入
 * {@link CurrentAdmin}。学员令牌在这里一律被拒绝，反之亦然。</p>
 *
 * @author fenglin
 */
@Component
public class AdminInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenService tokenService;
    private final AdminService adminService;
    private final ObjectMapper objectMapper;

    public AdminInterceptor(TokenService tokenService,
                            AdminService adminService,
                            ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.adminService = adminService;
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
            return reject(response, "尚未携带后台令牌，请先登录管理系统");
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        Long adminId = tokenService.parseAdminId(token);
        if (adminId == null) {
            return reject(response, "后台登录状态已失效，请重新登录");
        }
        DrAdmin admin = adminService.getAdminById(adminId);
        if (admin == null) {
            return reject(response, "管理员账号不存在或已被注销");
        }
        if (admin.getStatus() == null || admin.getStatus() == 0) {
            return reject(response, "该管理员账号已被封禁");
        }

        CurrentAdmin.set(admin);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 一定要清理，防止线程复用导致串号
        CurrentAdmin.clear();
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
