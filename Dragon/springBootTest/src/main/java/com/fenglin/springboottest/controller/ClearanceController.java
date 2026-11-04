package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.BizException;
import com.fenglin.springboottest.common.CurrentUser;
import com.fenglin.springboottest.common.R;
import com.fenglin.springboottest.entity.DrUser;
import com.fenglin.springboottest.service.ClearanceService;
import com.fenglin.springboottest.vo.ClearanceVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学员权限进度接口（需 JWT）。
 *
 * <pre>
 * GET /api/user/clearance   拉取当前账号的权限点 / 已完成任务
 * PUT /api/user/clearance   合并上报（多设备同步，返回合并后的结果）
 * </pre>
 *
 * <p>挂在 /api/user/** 下，由 {@code JwtInterceptor} 强制鉴权。
 * 作用是让权限进度跟着账号走，换电脑登录后自动恢复。</p>
 *
 * @author fenglin
 */
@RestController
@RequestMapping("/api/user/clearance")
public class ClearanceController {

    private final ClearanceService clearanceService;

    public ClearanceController(ClearanceService clearanceService) {
        this.clearanceService = clearanceService;
    }

    /** 读取权限进度 */
    @GetMapping
    public R<ClearanceVO> get() {
        DrUser user = currentUser();
        return R.ok(clearanceService.get(user.getId()));
    }

    /** 合并上报（并返回合并后的最终结果） */
    @PutMapping
    public R<ClearanceVO> save(@RequestBody(required = false) ClearanceVO body) {
        DrUser user = currentUser();
        return R.ok(clearanceService.mergeAndSave(user.getId(), user.getAccount(), body));
    }

    private DrUser currentUser() {
        DrUser user = CurrentUser.get();
        if (user == null || user.getId() == null) {
            throw new BizException(401, "登录状态已失效，请重新认证");
        }
        return user;
    }
}
