package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.CurrentAdmin;
import com.fenglin.springboottest.common.R;
import com.fenglin.springboottest.dto.AdminLoginDTO;
import com.fenglin.springboottest.service.AdminService;
import com.fenglin.springboottest.vo.AdminLoginVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台管理系统 · 认证接口，与前端 src/api/admin.js 一一对应。
 *
 * <pre>
 * POST /api/admin/auth/login   管理员登录（返回后台 JWT）
 * GET  /api/admin/auth/me      当前登录的管理员信息（前端校验会话用）
 * </pre>
 *
 * <p>后台只有登录、没有注册；账号由 sql/dragon_raja_admin.sql 预置。</p>
 *
 * @author fenglin
 */
@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AdminService adminService;

    public AdminAuthController(AdminService adminService) {
        this.adminService = adminService;
    }

    /** 管理员登录 */
    @PostMapping("/login")
    public R<AdminLoginVO> login(@Valid @RequestBody AdminLoginDTO dto) {
        return R.ok("登录成功，欢迎回到卡塞尔学院后台", adminService.login(dto));
    }

    /** 当前登录的管理员（拦截器已保证 CurrentAdmin 非空） */
    @GetMapping("/me")
    public R<AdminLoginVO.AdminInfo> me() {
        var admin = CurrentAdmin.get();
        return R.ok(new AdminLoginVO.AdminInfo(admin.getId(), admin.getUsername(), admin.getNickname()));
    }
}
