package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.R;
import com.fenglin.springboottest.dto.SpiritSaveDTO;
import com.fenglin.springboottest.entity.DrSpirit;
import com.fenglin.springboottest.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 后台管理系统 · 言灵图鉴数据管理接口，与前端 src/api/admin.js 一一对应。
 *
 * <pre>
 * GET    /api/admin/spirit/list   全部言灵（含隐藏，后台管理用）
 * POST   /api/admin/spirit        新增言灵
 * PUT    /api/admin/spirit/{id}   编辑言灵
 * DELETE /api/admin/spirit/{id}  删除言灵（逻辑删除）
 * </pre>
 *
 * @author fenglin
 */
@RestController
@RequestMapping("/api/admin/spirit")
public class AdminSpiritController {

    private final AdminService adminService;

    public AdminSpiritController(AdminService adminService) {
        this.adminService = adminService;
    }

    /** 后台列表 */
    @GetMapping("/list")
    public R<List<DrSpirit>> list() {
        return R.ok(adminService.listSpirits());
    }

    /** 新增 */
    @PostMapping
    public R<DrSpirit> create(@Valid @RequestBody SpiritSaveDTO dto) {
        return R.ok("言灵条目已录入图鉴", adminService.createSpirit(dto));
    }

    /** 编辑 */
    @PutMapping("/{id}")
    public R<DrSpirit> update(@PathVariable Long id, @Valid @RequestBody SpiritSaveDTO dto) {
        return R.ok("言灵条目已更新", adminService.updateSpirit(id, dto));
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        adminService.deleteSpirit(id);
        return R.ok("言灵条目已从图鉴移除", null);
    }
}
