package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.R;
import com.fenglin.springboottest.dto.LoginDTO;
import com.fenglin.springboottest.dto.RegisterDTO;
import com.fenglin.springboottest.service.UserService;
import com.fenglin.springboottest.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 骑士（用户）接口。
 *
 * <p>已由原来的 Thymeleaf 页面控制器改造为纯 REST 接口，返回统一 JSON，
 * 供 Vue 前端（假面骑士创骑 登录/注册页）调用。</p>
 *
 * <pre>
 * POST /api/user/register       注册
 * POST /api/user/login          登录（成功后在 Session 中记录登录态）
 * POST /api/user/logout         退出登录
 * GET  /api/user/me             查询当前登录用户
 * GET  /api/user/check-username 校验骑士名是否可用
 * GET  /api/user/list           骑士花名册（演示 MyBatis-Plus 查询）
 * </pre>
 *
 * @author fenglin
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** 注册 */
    @PostMapping("/register")
    public R<UserVO> register(@Valid @RequestBody RegisterDTO dto) {
        UserVO vo = userService.register(dto);
        return R.ok("入队认证成功，欢迎加入创骑战队！", vo);
    }

    /** 登录 */
    @PostMapping("/login")
    public R<UserVO> login(@Valid @RequestBody LoginDTO dto) {
        UserVO vo = userService.login(dto);
        return R.ok("最佳搭配！变身成功", vo);
    }

    /** 退出登录 */
    @PostMapping("/logout")
    public R<Void> logout() {
        userService.logout();
        return R.ok("已退出变身状态", null);
    }

    /** 当前登录用户；未登录返回 401 */
    @GetMapping("/me")
    public R<UserVO> me() {
        UserVO vo = userService.currentUser();
        if (vo == null) {
            return R.fail(401, "尚未登录");
        }
        return R.ok(vo);
    }

    /** 骑士名是否可用 */
    @GetMapping("/check-username")
    public R<Boolean> checkUsername(@RequestParam("username") String username) {
        if (username == null || username.isBlank()) {
            return R.fail("骑士名不能为空");
        }
        return R.ok(userService.isUsernameAvailable(username.trim()));
    }

    /** 骑士花名册 */
    @GetMapping("/list")
    public R<List<UserVO>> list() {
        return R.ok(userService.listUsers());
    }
}
