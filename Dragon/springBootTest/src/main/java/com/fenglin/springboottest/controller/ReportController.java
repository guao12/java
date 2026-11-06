package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.R;
import com.fenglin.springboottest.dto.ReportCreateDTO;
import com.fenglin.springboottest.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 读者侧举报接口。
 *
 * <pre>
 * POST /api/forum/reports   提交举报（需登录）
 * </pre>
 *
 * <p>挂 /api/forum 前缀是为了复用软鉴权 —— 未登录访问会被 Service 拦成 401，
 * 前端据此引导登录，和发帖 / 回帖的行为保持一致。</p>
 *
 * @author fenglin
 */
@RestController
@RequestMapping("/api/forum")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * 提交举报。
     *
     * <p>同一人对同一对象只能举报一次（唯一键兜底），重复提交返回友好提示。</p>
     */
    @PostMapping("/reports")
    public R<Long> report(@Valid @RequestBody ReportCreateDTO body) {
        return R.ok("已收到举报，我们会尽快核实", reportService.report(body));
    }
}
