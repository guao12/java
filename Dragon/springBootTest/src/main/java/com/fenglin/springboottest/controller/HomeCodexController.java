package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.R;
import com.fenglin.springboottest.entity.DrSpirit;
import com.fenglin.springboottest.service.AdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前台公开接口：言灵图鉴列表。
 *
 * <pre>
 * GET /api/home/spirits   首页「言灵图鉴」区块 + /spirit/:key 详情页共用
 * </pre>
 *
 * <p>挂在 /api/home/** 下，走 OptionalAuthInterceptor 软鉴权：
 * 无需登录即可访问；后端不可用时前端会回退到内置静态数据，页面不白屏。</p>
 *
 * @author fenglin
 */
@RestController
@RequestMapping("/api/home")
public class HomeCodexController {

    private final AdminService adminService;

    public HomeCodexController(AdminService adminService) {
        this.adminService = adminService;
    }

    /** 言灵图鉴（仅展示中的条目，按 sort 排序） */
    @GetMapping("/spirits")
    public R<List<DrSpirit>> spirits() {
        return R.ok(adminService.publicSpirits());
    }
}
