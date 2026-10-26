package com.fenglin.springboottest.service;

import com.fenglin.springboottest.dto.LoginDTO;
import com.fenglin.springboottest.dto.RegisterDTO;
import com.fenglin.springboottest.dto.ResetConfirmDTO;
import com.fenglin.springboottest.entity.DrUser;
import com.fenglin.springboottest.vo.CheckAccountVO;
import com.fenglin.springboottest.vo.LoginVO;
import com.fenglin.springboottest.vo.ProfileVO;
import com.fenglin.springboottest.vo.RegisterCodeVO;
import com.fenglin.springboottest.vo.RegisterVO;
import com.fenglin.springboottest.vo.SendCodeVO;

/**
 * 认证业务接口：注册 / 登录 / 账号探测 / 密钥重置。
 * 与前端 DragonRaja-Vue 的 src/api/auth.js 一一对应。
 *
 * @author fenglin
 */
public interface AuthService {

    /** 新生登记：校验验证码 → 校验唯一性 → 签发学号 → BCrypt 加密 → 落库 */
    RegisterVO register(RegisterDTO dto);

    /** 下发注册验证码（5 分钟有效，60 秒冷却），演示环境回显验证码 */
    RegisterCodeVO sendRegisterCode(String clientKey);

    /** 身份认证：学号查找 → 校验密钥 → 累计登录 → 签发 JWT */
    LoginVO login(LoginDTO dto);

    /** 账号是否已登记（返回行动代号） */
    CheckAccountVO checkAccount(String account);

    /** 下发 6 位验证龙文（5 分钟有效，60 秒冷却），演示环境回显验证码 */
    SendCodeVO sendResetCode(String account);

    /** 校验验证龙文并重置言灵密钥 */
    void confirmReset(ResetConfirmDTO dto);

    /** 按主键取在册用户（逻辑删除自动过滤） */
    DrUser getUserById(Long userId);

    /** 把实体转成身份卡 VO（填充字典中文名） */
    ProfileVO toProfile(DrUser user);
}
