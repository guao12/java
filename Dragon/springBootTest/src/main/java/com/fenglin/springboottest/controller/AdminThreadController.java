package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.R;
import com.fenglin.springboottest.dto.ThreadAdminSaveDTO;
import com.fenglin.springboottest.entity.DrBoard;
import com.fenglin.springboottest.service.AdminThreadService;
import com.fenglin.springboottest.vo.AdminThreadVO;
import com.fenglin.springboottest.vo.PageVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台管理系统 · 论坛主题管理接口，与前端 src/api/admin.js 一一对应。
 *
 * <pre>
 * GET    /api/admin/threads           主题分页（boardId / status / keyword 均可空）
 * GET    /api/admin/threads/boards    版块下拉
 * POST   /api/admin/threads           新建主题
 * PUT    /api/admin/threads/{id}      编辑主题（含置顶 / 加精 / 上下线）
 * DELETE /api/admin/threads/{id}      删除主题（逻辑删除）
 * </pre>
 *
 * <p>/api/admin/** 由 AdminInterceptor 校验管理员令牌，无需额外鉴权。
 * 注意 {@code /boards} 必须定义在 {@code /{id}} 之前，否则 "boards" 会被当成主键解析。</p>
 *
 * @author fenglin
 */
@RestController
@RequestMapping("/api/admin/threads")
public class AdminThreadController {

    private final AdminThreadService adminThreadService;

    public AdminThreadController(AdminThreadService adminThreadService) {
        this.adminThreadService = adminThreadService;
    }

    /**
     * 主题分页。
     *
     * @param boardId 版块主键，不传 = 全部
     * @param status  1 正常 / 0 待审 / -1 已下线，不传 = 全部
     * @param keyword 标题 / 正文 / 作者
     */
    @GetMapping
    public R<PageVO<AdminThreadVO>> list(@RequestParam(value = "boardId", required = false) Long boardId,
                                         @RequestParam(value = "status", required = false) Integer status,
                                         @RequestParam(value = "keyword", required = false) String keyword,
                                         @RequestParam(value = "page", defaultValue = "1") long page,
                                         @RequestParam(value = "size", defaultValue = "20") long size) {
        return R.ok(adminThreadService.list(boardId, status, keyword, page, size));
    }

    /** 版块下拉（含已关闭版块） */
    @GetMapping("/boards")
    public R<List<DrBoard>> boards() {
        return R.ok(adminThreadService.boards());
    }

    /** 新建 */
    @PostMapping
    public R<AdminThreadVO> create(@Valid @RequestBody ThreadAdminSaveDTO dto) {
        return R.ok("主题已发布", adminThreadService.create(dto));
    }

    /** 编辑 */
    @PutMapping("/{id}")
    public R<AdminThreadVO> update(@PathVariable("id") Long id,
                                   @Valid @RequestBody ThreadAdminSaveDTO dto) {
        return R.ok("主题已更新", adminThreadService.update(id, dto));
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable("id") Long id) {
        adminThreadService.delete(id);
        return R.ok("主题已删除", null);
    }
}
