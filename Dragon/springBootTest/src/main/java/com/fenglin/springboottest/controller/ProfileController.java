package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.BizException;
import com.fenglin.springboottest.common.CurrentUser;
import com.fenglin.springboottest.common.R;
import com.fenglin.springboottest.entity.DrUser;
import com.fenglin.springboottest.service.AuthService;
import com.fenglin.springboottest.vo.ProfileVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学院内网接口（需 JWT）。
 *
 * <pre>
 * GET  /api/user/profile   我的身份卡（档案来自 MySQL · dr_user）
 * POST /api/user/logout    退出登录（无状态 JWT：前端清除本地令牌即可）
 * </pre>
 *
 * <p>鉴权由 {@code JwtInterceptor} 统一完成（拦截 /api/user/**），
 * 控制器里直接从 {@link CurrentUser} 取当前登录用户。</p>
 *
 * @author fenglin
 */
@RestController
@RequestMapping("/api/user")
public class ProfileController {

    private final AuthService authService;

    public ProfileController(AuthService authService) {
        this.authService = authService;
    }

    /** 我的身份卡 */
    @GetMapping("/profile")
    public R<ProfileVO> profile() {
        DrUser user = CurrentUser.get();
        if (user == null) {
            // 理论上到不了这里：拦截器已保证登录态
            throw new BizException(401, "登录状态已失效，请重新认证");
        }
        return R.ok(authService.toProfile(user));
    }

    /** 退出登录 */
    @PostMapping("/logout")
    public R<Void> logout() {
        DrUser user = CurrentUser.get();
        CurrentUser.clear();
        if (user != null) {
            // 无状态 JWT：服务端无需注销令牌，仅记录日志
            // 如需强制失效可在此维护令牌黑名单（Redis / 内存 Map）
        }
        return R.ok("已退出学院内网", null);
    }
}
