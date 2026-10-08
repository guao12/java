package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.common.BizException;
import com.fenglin.springboottest.common.SessionConst;
import com.fenglin.springboottest.dto.LoginDTO;
import com.fenglin.springboottest.dto.RegisterDTO;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.UserMapper;
import com.fenglin.springboottest.service.UserService;
import com.fenglin.springboottest.vo.UserVO;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户业务实现。
 *
 * @author fenglin
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    /** 默认形态 */
    private static final String DEFAULT_FORM = "rabbittank";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    /** Spring 为单例 Bean 注入的是 Session 的代理对象，调用时才取当前请求的 Session */
    private final HttpSession session;

    public UserServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder, HttpSession session) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.session = session;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO register(RegisterDTO dto) {
        // 1. 两次密码一致性（后端兜底校验）
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BizException("两次输入的变身密码不一致");
        }

        // 2. 唯一性校验
        if (userMapper.countByUsername(dto.getUsername()) > 0) {
            throw new BizException("骑士名「" + dto.getUsername() + "」已被占用，换一个吧");
        }
        if (userMapper.countByEmail(dto.getEmail()) > 0) {
            throw new BizException("邮箱「" + dto.getEmail() + "」已被注册");
        }

        // 3. 组装实体并加密密码
        String formKey = (dto.getFormKey() == null || dto.getFormKey().isBlank())
                ? DEFAULT_FORM : dto.getFormKey();
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setFormKey(formKey);
        user.setStatus(1);

        // 4. 落库（MyBatis-Plus BaseMapper.insert）
        int rows = userMapper.insert(user);
        if (rows != 1) {
            throw new BizException(500, "注册失败，请稍后重试");
        }
        log.info("新骑士入队：{}（id={}）", user.getUsername(), user.getId());

        // 5. 回查一次，拿到数据库生成的 create_time
        User saved = userMapper.selectById(user.getId());
        return UserVO.from(saved == null ? user : saved);
    }

    @Override
    public UserVO login(LoginDTO dto) {
        // 1. 按用户名查（逻辑删除的用户查不到）
        User user = userMapper.selectByUsername(dto.getUsername());
        // 统一提示语，避免暴露"该用户名是否存在"
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BizException("骑士名或变身密码不正确");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException("该骑士已被封印（账号禁用），请联系总部");
        }

        // 2. 登录成功，写入 Session
        session.setAttribute(SessionConst.LOGIN_USER, UserVO.from(user));
        log.info("骑士变身成功：{}（id={}）", user.getUsername(), user.getId());
        return UserVO.from(user);
    }

    @Override
    public UserVO currentUser() {
        Object attr = session.getAttribute(SessionConst.LOGIN_USER);
        return attr instanceof UserVO vo ? vo : null;
    }

    @Override
    public void logout() {
        session.removeAttribute(SessionConst.LOGIN_USER);
    }

    @Override
    public boolean isUsernameAvailable(String username) {
        return userMapper.countByUsername(username) == 0;
    }

    @Override
    public List<UserVO> listUsers() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .select(User::getId, User::getUsername, User::getEmail,
                        User::getFormKey, User::getStatus, User::getCreateTime)
                .orderByDesc(User::getId)
                .last("LIMIT 100");
        return userMapper.selectList(wrapper).stream().map(UserVO::from).collect(Collectors.toList());
    }
}
