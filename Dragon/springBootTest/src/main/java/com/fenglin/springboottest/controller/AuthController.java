package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.R;
import com.fenglin.springboottest.dto.LoginDTO;
import com.fenglin.springboottest.dto.RegisterDTO;
import com.fenglin.springboottest.dto.RegisterSendDTO;
import com.fenglin.springboottest.dto.ResetConfirmDTO;
import com.fenglin.springboottest.dto.ResetSendDTO;
import com.fenglin.springboottest.service.AuthService;
import com.fenglin.springboottest.service.DictService;
import com.fenglin.springboottest.vo.CheckAccountVO;
import com.fenglin.springboottest.vo.LoginVO;
import com.fenglin.springboottest.vo.RegisterCodeVO;
import com.fenglin.springboottest.vo.RegisterVO;
import com.fenglin.springboottest.vo.SendCodeVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口，与前端 DragonRaja-Vue 的 src/api/auth.js 一一对应。
 *
 * <pre>
 * POST /api/auth/register        新生登记（签发学号）
 * POST /api/auth/register/send   下发注册验证码（系统直接给验证码，不发邮件）
 * POST /api/auth/login           身份认证（返回 JWT + 身份卡）
 * GET  /api/auth/check-account   账号是否已登记（脱敏邮箱）
 * POST /api/auth/reset/send      下发验证龙文（5 分钟有效，60 秒冷却）
 * POST /api/auth/reset/confirm   校验验证龙文并重置密钥
 * GET  /api/auth/dictionaries   在校身份 / 言灵字典（前后端共用 code）
 * </pre>
 *
 * @author fenglin
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final DictService dictService;

    public AuthController(AuthService authService, DictService dictService) {
        this.authService = authService;
        this.dictService = dictService;
    }

    /** 新生登记 */
    @PostMapping("/register")
    public R<RegisterVO> register(@Valid @RequestBody RegisterDTO dto) {
        return R.ok("登记完成，学院编号已签发", authService.register(dto));
    }

    /** 下发注册验证码：演示环境由系统直接给出 6 位验证码 */
    @PostMapping("/register/send")
    public R<RegisterCodeVO> sendRegisterCode(@Valid @RequestBody RegisterSendDTO dto) {
        return R.ok("验证码已生成", authService.sendRegisterCode(dto.getClientKey()));
    }

    /** 身份认证 */
    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return R.ok("身份认证通过，欢迎回到卡塞尔学院", authService.login(dto));
    }

    /** 账号是否已登记 */
    @GetMapping("/check-account")
    public R<CheckAccountVO> checkAccount(@RequestParam("account") String account) {
        return R.ok(authService.checkAccount(account));
    }

    /** 下发验证龙文 */
    @PostMapping("/reset/send")
    public R<SendCodeVO> sendResetCode(@Valid @RequestBody ResetSendDTO dto) {
        return R.ok("重置指令已下发", authService.sendResetCode(dto.getAccount()));
    }

    /** 校验验证龙文并重置密钥 */
    @PostMapping("/reset/confirm")
    public R<Void> confirmReset(@Valid @RequestBody ResetConfirmDTO dto) {
        authService.confirmReset(dto);
        return R.ok("言灵密钥已重铸，请使用新密钥登录", null);
    }

    /** 字典：在校身份 / 言灵 */
    @GetMapping("/dictionaries")
    public R<Object> dictionaries() {
        return R.ok(dictService.dictionaries());
    }
}
