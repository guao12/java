package com.fenglin.springboottest.service;

import com.fenglin.springboottest.dto.LoginDTO;
import com.fenglin.springboottest.dto.RegisterDTO;
import com.fenglin.springboottest.vo.UserVO;

import java.util.List;

/**
 * 用户业务接口。
 *
 * @author fenglin
 */
public interface UserService {

    /** 注册：校验唯一性 → 加密密码 → 落库 */
    UserVO register(RegisterDTO dto);

    /** 登录：查用户 → 校验密码 → 把用户写入 Session */
    UserVO login(LoginDTO dto);

    /** 取当前登录用户，未登录返回 null */
    UserVO currentUser();

    /** 退出登录 */
    void logout();

    /** 骑士名是否可用 */
    boolean isUsernameAvailable(String username);

    /** 骑士花名册（不含密码），按创建时间倒序 */
    List<UserVO> listUsers();
}
