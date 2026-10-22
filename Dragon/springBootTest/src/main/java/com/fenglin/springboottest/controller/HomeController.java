package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.R;
import com.fenglin.springboottest.service.HomeService;
import com.fenglin.springboottest.vo.HomeOverviewVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页展示接口。
 *
 * <p>首页需要的数据（学院通报 / 统计面板 / 两级分类 / 精选 · 热门 · 最新 / 热词）
 * 由 {@code /api/home/overview} 一次返回，前端不必并发多个请求。</p>
 *
 * <p>接口对所有人开放，但返回内容按访问者身份的可见密级过滤，
 * 见 {@code OptionalAuthInterceptor} 与 {@code SecurityLevel}。</p>
 *
 * @author fenglin
 */
@RestController
@RequestMapping("/api/home")
public class HomeController {

    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    /**
     * 首页概览。
     *
     * <pre>
     * GET /api/home/overview
     * </pre>
     */
    @GetMapping("/overview")
    public R<HomeOverviewVO> overview() {
        return R.ok(homeService.overview());
    }
}
