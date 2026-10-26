package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fenglin.springboottest.common.BizException;
import com.fenglin.springboottest.dto.LoginDTO;
import com.fenglin.springboottest.dto.RegisterDTO;
import com.fenglin.springboottest.dto.ResetConfirmDTO;
import com.fenglin.springboottest.entity.DrResetCode;
import com.fenglin.springboottest.entity.DrUser;
import com.fenglin.springboottest.mapper.DrResetCodeMapper;
import com.fenglin.springboottest.mapper.DrUserMapper;
import com.fenglin.springboottest.service.AuthService;
import com.fenglin.springboottest.service.DictService;
import com.fenglin.springboottest.service.TokenService;
import com.fenglin.springboottest.vo.CheckAccountVO;
import com.fenglin.springboottest.vo.LoginVO;
import com.fenglin.springboottest.vo.ProfileVO;
import com.fenglin.springboottest.vo.RegisterCodeVO;
import com.fenglin.springboottest.vo.RegisterVO;
import com.fenglin.springboottest.vo.SendCodeVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.Year;

/**
 * 认证业务实现。
 *
 * @author fenglin
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    /** 验证龙文有效期（秒）与冷却（秒） */
    private static final int CODE_EXPIRE_SECONDS = 300;
    private static final int CODE_COOLDOWN_SECONDS = 60;

    /**
     * 注册验证码在 dr_reset_code 表中的 account 前缀。
     * 真实学号固定是 CC + 年份 + 5 位序号，用 REG 前缀可保证不会与之冲突；
     * 表结构未改动，注册验证码与重置验证码共用一张表、靠前缀区分场景。
     */
    private static final String REGISTER_KEY_PREFIX = "REG";

    /** account 列长度上限（VARCHAR(20)），前缀占 3 位 */
    private static final int REGISTER_KEY_BODY_MAX = 17;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final DrUserMapper userMapper;
    private final DrResetCodeMapper resetCodeMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final DictService dictService;

    public AuthServiceImpl(DrUserMapper userMapper,
                           DrResetCodeMapper resetCodeMapper,
                           PasswordEncoder passwordEncoder,
                           TokenService tokenService,
                           DictService dictService) {
        this.userMapper = userMapper;
        this.resetCodeMapper = resetCodeMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.dictService = dictService;
    }

    // ------------------------------------------------------------
    // 注册
    // ------------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegisterVO register(RegisterDTO dto) {
        String codename = dto.getCodename().trim();

        // 0. 验证码校验（替代原来的邮箱验证：验证码由系统下发并直接回显，用户照填）
        //    校验通过后不立刻作废，等登记成功时再统一作废；本条事务回滚时验证码仍可复用
        String regKey = registerKey(dto.getClientKey());
        verifyRegisterCode(regKey, dto.getCode());

        // 1. 字典合法性
        if (!dictService.isValidIdentity(dto.getIdentity())) {
            throw new BizException("在校身份不合法，请重新选择");
        }
        String spirit = (dto.getSpirit() == null || dto.getSpirit().isBlank())
                ? "none" : dto.getSpirit();
        if (!dictService.isValidSpirit(spirit)) {
            throw new BizException("言灵不合法，请重新选择");
        }

        // 2. 唯一性校验
        Long sameName = userMapper.selectCount(new LambdaQueryWrapper<DrUser>()
                .eq(DrUser::getCodename, codename));
        if (sameName != null && sameName > 0) {
            throw new BizException("行动代号「" + codename + "」已被使用，换一个吧");
        }

        // 3. 组装实体：签发学号 + BCrypt 加密
        DrUser user = new DrUser();
        user.setAccount(issueAccount());
        user.setCodename(codename);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setIdentity(dto.getIdentity());
        user.setSpirit(spirit);
        user.setStatus(1);
        user.setLoginCount(0);

        // 4. 落库（唯一键兜底，重复时重试一次学号）
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            user.setAccount(issueAccount());
            userMapper.insert(user);
        }
        log.info("新生登记完成：{}（学号={}）", user.getCodename(), user.getAccount());

        // 5. 登记成功，作废该浏览器标识下所有未使用的验证码
        resetCodeMapper.update(null, new LambdaUpdateWrapper<DrResetCode>()
                .eq(DrResetCode::getAccount, regKey)
                .eq(DrResetCode::getUsed, 0)
                .set(DrResetCode::getUsed, 1));

        RegisterVO vo = new RegisterVO();
        vo.setAccount(user.getAccount());
        vo.setCodename(user.getCodename());
        vo.setIdentityName(dictService.identityName(user.getIdentity()));
        vo.setSpiritName(dictService.spiritName(user.getSpirit()));
        return vo;
    }

    /** 签发学院编号：CC + 年份 + 5 位序号，如 CC20260001 */
    private String issueAccount() {
        String year = String.valueOf(Year.now().getValue());
        Integer maxSeq = userMapper.maxAccountSeqOfYear(year);
        int next = (maxSeq == null ? 0 : maxSeq) + 1;
        return String.format("CC%s%05d", year, next);
    }

    // ------------------------------------------------------------
    // 注册验证码（替代邮箱验证：系统直接给验证码）
    // ------------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegisterCodeVO sendRegisterCode(String clientKey) {
        String key = registerKey(clientKey);

        // 60 秒冷却：同一浏览器标识一分钟内不能重复下发
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(CODE_COOLDOWN_SECONDS);
        Long recent = resetCodeMapper.selectCount(new LambdaQueryWrapper<DrResetCode>()
                .eq(DrResetCode::getAccount, key)
                .ge(DrResetCode::getCreateTime, threshold));
        if (recent != null && recent > 0) {
            throw new BizException("验证码获取过于频繁，请 1 分钟后再试");
        }

        // 生成 6 位数字验证码并落库
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        DrResetCode entity = new DrResetCode();
        entity.setAccount(key);
        entity.setCode(code);
        entity.setExpireTime(LocalDateTime.now().plusSeconds(CODE_EXPIRE_SECONDS));
        entity.setUsed(0);
        resetCodeMapper.insert(entity);

        log.info("注册验证码已下发：key={}（演示环境直接回显，不发邮件）", key);
        // 真实项目在这里接短信 / 邮件服务；演示环境由系统直接给出验证码
        return new RegisterCodeVO(code, CODE_EXPIRE_SECONDS, CODE_COOLDOWN_SECONDS);
    }

    /** 校验注册验证码：必须存在、未使用、未过期 */
    private void verifyRegisterCode(String key, String code) {
        if (code == null || code.isBlank()) {
            throw new BizException("请输入验证码");
        }
        DrResetCode match = resetCodeMapper.selectOne(new LambdaQueryWrapper<DrResetCode>()
                .eq(DrResetCode::getAccount, key)
                .eq(DrResetCode::getCode, code.trim())
                .eq(DrResetCode::getUsed, 0)
                .gt(DrResetCode::getExpireTime, LocalDateTime.now())
                .orderByDesc(DrResetCode::getId)
                .last("LIMIT 1"));
        if (match == null) {
            throw new BizException("验证码错误或已过期，请重新获取");
        }
    }

    /**
     * 把浏览器标识规整成 dr_reset_code.account 里存储的键：REG + 最多 17 位字母数字。
     *
     * @param raw 前端生成的 clientKey
     * @return 如 REGa1b2c3d4e5f60718（长度不超过 VARCHAR(20)）
     */
    private String registerKey(String raw) {
        String cleaned = raw == null ? "" : raw.replaceAll("[^A-Za-z0-9]", "");
        if (cleaned.isEmpty()) {
            throw new BizException("缺少浏览器标识，请刷新页面后重试");
        }
        if (cleaned.length() > REGISTER_KEY_BODY_MAX) {
            cleaned = cleaned.substring(cleaned.length() - REGISTER_KEY_BODY_MAX);
        }
        return REGISTER_KEY_PREFIX + cleaned;
    }

    // ------------------------------------------------------------
    // 登录
    // ------------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO login(LoginDTO dto) {
        String input = dto.getAccount().trim();

        // 1. 按学号或行动代号（姓名）查找（二者在库中均唯一）
        DrUser user = resolveUser(input);
        // 统一提示语，避免暴露"该账号是否存在"
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BizException("学号或言灵密钥不正确");
        }
        if (user.getStatus() == null || user.getStatus() == 0) {
            throw new BizException("该身份卡已被学院封印，请联系执行部");
        }

        // 2. 累计登录次数 + 最近登录时间
        userMapper.update(null, new LambdaUpdateWrapper<DrUser>()
                .eq(DrUser::getId, user.getId())
                .setSql("login_count = login_count + 1")
                .set(DrUser::getLastLoginAt, LocalDateTime.now()));
        user.setLoginCount((user.getLoginCount() == null ? 0 : user.getLoginCount()) + 1);
        user.setLastLoginAt(LocalDateTime.now());

        // 3. 签发 JWT（勾选"记住身份卡"时使用长令牌）
        boolean remember = Boolean.TRUE.equals(dto.getRemember());
        String token = tokenService.issue(user.getId(), user.getAccount(), remember);

        log.info("身份认证通过：{}（学号={}）", user.getCodename(), user.getAccount());
        return new LoginVO(token, tokenService.expiresIn(remember), toProfile(user));
    }

    // ------------------------------------------------------------
    // 账号探测
    // ------------------------------------------------------------

    @Override
    public CheckAccountVO checkAccount(String account) {
        String input = account == null ? "" : account.trim();
        if (input.isEmpty()) {
            throw new BizException("请输入学号或姓名");
        }
        DrUser user = resolveUser(input);
        if (user == null) {
            return new CheckAccountVO(false, null);
        }
        return new CheckAccountVO(true, user.getCodename());
    }

    // ------------------------------------------------------------
    // 密钥重置
    // ------------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SendCodeVO sendResetCode(String account) {
        DrUser user = resolveUser(account);
        if (user == null) {
            throw new BizException("该身份卡尚未登记，请先完成新生登记");
        }

        // 60 秒冷却：同一账号一分钟内不能重复下发
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(CODE_COOLDOWN_SECONDS);
        Long recent = resetCodeMapper.selectCount(new LambdaQueryWrapper<DrResetCode>()
                .eq(DrResetCode::getAccount, user.getAccount())
                .ge(DrResetCode::getCreateTime, threshold));
        if (recent != null && recent > 0) {
            throw new BizException("验证龙文发送过于频繁，请 1 分钟后再试");
        }

        // 生成 6 位数字验证码并落库
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        DrResetCode entity = new DrResetCode();
        entity.setAccount(user.getAccount());
        entity.setCode(code);
        entity.setExpireTime(LocalDateTime.now().plusSeconds(CODE_EXPIRE_SECONDS));
        entity.setUsed(0);
        resetCodeMapper.insert(entity);

        log.info("验证龙文已下发：学号={}（演示环境回显）", user.getAccount());
        // 真实项目在这里接邮件 / 短信服务；演示环境直接回显给前端
        return new SendCodeVO(user.getAccount(), code, CODE_EXPIRE_SECONDS);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReset(ResetConfirmDTO dto) {
        DrUser user = resolveUser(dto.getAccount());
        if (user == null) {
            throw new BizException("该身份卡尚未登记，请先完成新生登记");
        }

        // 1. 找到匹配且未使用且未过期的最新一条验证码
        DrResetCode match = resetCodeMapper.selectOne(new LambdaQueryWrapper<DrResetCode>()
                .eq(DrResetCode::getAccount, user.getAccount())
                .eq(DrResetCode::getCode, dto.getCode())
                .eq(DrResetCode::getUsed, 0)
                .gt(DrResetCode::getExpireTime, LocalDateTime.now())
                .orderByDesc(DrResetCode::getId)
                .last("LIMIT 1"));
        if (match == null) {
            throw new BizException("验证龙文错误或已过期，请重新获取");
        }

        // 2. 重置密钥
        userMapper.update(null, new LambdaUpdateWrapper<DrUser>()
                .eq(DrUser::getId, user.getId())
                .set(DrUser::getPassword, passwordEncoder.encode(dto.getPassword())));

        // 3. 该账号所有验证码立即作废，防止重复使用
        resetCodeMapper.update(null, new LambdaUpdateWrapper<DrResetCode>()
                .eq(DrResetCode::getAccount, user.getAccount())
                .eq(DrResetCode::getUsed, 0)
                .set(DrResetCode::getUsed, 1));

        log.info("言灵密钥已重铸：学号={}", user.getAccount());
    }

    /**
     * 身份标识 → 用户：先按学号匹配，再按行动代号（姓名）匹配。
     * 二者在库中均唯一（学号由系统签发、代号注册时去重），故可唯一确定一位用户。
     * 重置密钥、校验身份等场景统一走这里，支持"凭学号或姓名即可操作"。
     */
    private DrUser resolveUser(String identifier) {
        String input = identifier == null ? "" : identifier.trim();
        if (input.isEmpty()) {
            return null;
        }
        DrUser user = userMapper.selectByAccount(input);
        if (user == null) {
            user = userMapper.selectByCodename(input);
        }
        return user;
    }

    // ------------------------------------------------------------
    // 公共
    // ------------------------------------------------------------

    @Override
    public DrUser getUserById(Long userId) {
        if (userId == null) {
            return null;
        }
        return userMapper.selectById(userId);
    }

    @Override
    public ProfileVO toProfile(DrUser user) {
        return ProfileVO.from(user,
                dictService.identityName(user.getIdentity()),
                dictService.spiritName(user.getSpirit()));
    }
}
