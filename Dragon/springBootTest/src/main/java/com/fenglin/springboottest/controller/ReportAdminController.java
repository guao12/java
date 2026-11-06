package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.R;
import com.fenglin.springboottest.service.ReportService;
import com.fenglin.springboottest.vo.PageVO;
import com.fenglin.springboottest.vo.ReportVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台管理系统 · 举报处理接口，与前端 src/api/admin.js 一一对应。
 *
 * <pre>
 * GET  /api/admin/reports                  队列分页（status：0 待处理 / 1 已下架 / 2 已驳回 / -1 全部）
 * GET  /api/admin/reports/pending-count   待处理数量（侧栏角标）
 * POST /api/admin/reports/{id}/handle     处理（action：takedown / reject）
 * </pre>
 *
 * <p>/api/admin/** 由 AdminInterceptor 校验管理员令牌，无需额外鉴权。</p>
 *
 * @author fenglin
 */
@RestController
@RequestMapping("/api/admin/reports")
public class ReportAdminController {

    private final ReportService reportService;

    public ReportAdminController(ReportService reportService) {
        this.reportService = reportService;
    }

    /** 队列分页 */
    @GetMapping
    public R<PageVO<ReportVO>> list(@RequestParam(value = "status", defaultValue = "0") int status,
                                    @RequestParam(value = "page", defaultValue = "1") long page,
                                    @RequestParam(value = "size", defaultValue = "20") long size) {
        return R.ok(reportService.list(status, page, size));
    }

    /** 待处理数量 */
    @GetMapping("/pending-count")
    public R<Long> pendingCount() {
        return R.ok(reportService.pendingCount());
    }

    /**
     * 处理工单。
     *
     * @param action takedown（下架）/ reject（驳回）
     * @param note   处理备注，可空
     */
    @PostMapping("/{id}/handle")
    public R<Void> handle(@PathVariable("id") Long id,
                          @RequestParam("action") String action,
                          @RequestParam(value = "note", required = false) String note) {
        reportService.handle(id, action, note);
        return R.ok("takedown".equalsIgnoreCase(action) ? "已下架" : "已驳回", null);
    }
}
