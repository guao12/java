package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fenglin.springboottest.common.BizException;
import com.fenglin.springboottest.dto.LoginDTO;
import com.fenglin.springboottest.dto.RegisterDTO;
import com.fenglin.springboottest.dto.RegisterSendDTO;
import com.fenglin.springboottest.dto.ResetConfirmDTO;
import com.fenglin.springboottest.entity.DrResetCode;
import com.fenglin.springboottest.entity.DrUser;
import com.fenglin.springboottest.mapper.DrResetCodeMapper;
import com.fenglin.springboottest.mapper.DrUserMapper;
import com.fenglin.springboottest.service.AuthService;
import com.fenglin.springboottest.service.CaptchaService;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    private final CaptchaService captchaService;

    /** 注册验证码下发冷却：按客户端真实 IP 限流（不再依赖前端 clientKey，防换 key 绕过） */
    private final Map<String, Long> registerIpCooldown = new ConcurrentHashMap<>();

    /** 验证龙文下发冷却：按客户端真实 IP 限流（与注册同源，防脚本批量探测 / 刷取） */
    private final Map<String, Long> resetIpCooldown = new ConcurrentHashMap<>();

    public AuthServiceImpl(DrUserMapper userMapper,
                           DrResetCodeMapper resetCodeMapper,
                           PasswordEncoder passwordEncoder,
                           TokenService tokenService,
                           DictService dictService,
                           CaptchaService captchaService) {
        this.userMapper = userMapper;
        this.resetCodeMapper = resetCodeMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.dictService = dictService;
        this.captchaService = captchaService;
    }

    // ------------------------------------------------------------
    // 注册
    // ------------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegisterVO register(RegisterDTO dto) {
        String codename = dto.getCodename().trim();

        // 0. 验证码校验（替代原来的邮箱验证：验证码由系统下发并直接回显，用户照填）
        //    校验通过后立即消费该验证码，防止同一验证码被重放 / 反复尝试不同代号
        String regKey = registerKey(dto.getClientKey());
        DrResetCode matchedCode = verifyRegisterCode(regKey, dto.getCode());
        matchedCode.setUsed(1);
        resetCodeMapper.updateById(matchedCode);

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

        // 4. 落库（codename 已加唯一索引；并发冲突由唯一键兜底，直接提示换代号）
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            throw new BizException("行动代号「" + codename + "」已被使用，换一个吧");
        }
        log.info("新生登记完成：{}（学号={}）", user.getCodename(), user.getAccount());

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
    public RegisterCodeVO sendRegisterCode(RegisterSendDTO dto, String clientIp) {
        // 1. 服务端 IP 限流：同一真实 IP 60 秒内只能下发一次。
        //    关键修复：原实现按前端 clientKey 限流，攻击者换 key 即可无限绕过；
        //    现改为按服务端可信的客户端 IP 限流，clientKey 仅用于把验证码绑定到本次登记会话。
        long now = System.currentTimeMillis();
        Long last = registerIpCooldown.get(clientIp);
        if (last != null && now - last < CODE_COOLDOWN_SECONDS * 1000L) {
            throw new BizException("验证码获取过于频繁，请 1 分钟后再试");
        }

        // 2. 图形验证码：必须先通过，校验失败直接拦截（不消耗验证码，便于用户更正后重试）
        if (!captchaService.verify(dto.getCaptchaToken(), dto.getCaptchaCode())) {
            throw new BizException("图形验证码错误或已过期，请点击图片刷新后重试");
        }

        // 3. 生成 6 位数字验证码并落库（绑定到本次登记会话的 registerKey）
        String key = registerKey(dto.getClientKey());
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        DrResetCode entity = new DrResetCode();
        entity.setAccount(key);
        entity.setCode(code);
        entity.setExpireTime(LocalDateTime.now().plusSeconds(CODE_EXPIRE_SECONDS));
        entity.setUsed(0);
        resetCodeMapper.insert(entity);

        // 4. 记录本 IP 的冷却起点（图形验证码已在第 2 步校验成功时消费，不可复用）
        registerIpCooldown.put(clientIp, now);
        // 惰性清理：超过容量上限时剔除已过期条目，避免内存无限增长
        if (registerIpCooldown.size() > 1024) {
            long expireBefore = now - CODE_COOLDOWN_SECONDS * 1000L;
            registerIpCooldown.values().removeIf(ts -> ts < expireBefore);
        }

        log.info("注册验证码已下发：key={}（演示环境直接回显，不发邮件）", key);
        // 真实项目在这里接短信 / 邮件服务；演示环境由系统直接给出验证码
        return new RegisterCodeVO(code, CODE_EXPIRE_SECONDS, CODE_COOLDOWN_SECONDS);
    }

    /** 校验注册验证码：必须存在、未使用、未过期；校验通过返回该条记录（供调用方立即消费） */
    private DrResetCode verifyRegisterCode(String key, String code) {
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
        return match;
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
        // 安全：该接口早期会回显 registered / 真实行动代号，可被批量探测"哪些账号已注册"。
        // 现改为统一模糊返回 —— 无论账号是否存在都返回相同结构，杜绝账号枚举。
        // （真正的"未登记"提示改由 sendResetCode / confirmReset 在提交时统一给出。）
        return new CheckAccountVO(true, null);
    }

    // ------------------------------------------------------------
    // 密钥重置
    // ------------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SendCodeVO sendResetCode(ResetSendDTO dto, String clientIp) {
        // 1. 服务端 IP 限流：同一真实 IP 60 秒内只能下发一次
        long now = System.currentTimeMillis();
        Long last = resetIpCooldown.get(clientIp);
        if (last != null && now - last < CODE_COOLDOWN_SECONDS * 1000L) {
            throw new BizException("验证龙文发送过于频繁，请 1 分钟后再试");
        }

        // 2. 图形验证码：必须先通过，校验失败直接拦截（不消耗验证码，便于用户更正后重试）
        if (!captchaService.verify(dto.getCaptchaToken(), dto.getCaptchaCode())) {
            throw new BizException("图形验证码错误或已过期，请点击图片刷新后重试");
        }

        DrUser user = resolveUser(dto.getAccount());
        String code;
        if (user != null) {
            // 已登记：生成验证龙文并落库（5 分钟有效）
            code = String.format("%06d", RANDOM.nextInt(1_000_000));
            DrResetCode entity = new DrResetCode();
            entity.setAccount(user.getAccount());
            entity.setCode(code);
            entity.setExpireTime(LocalDateTime.now().plusSeconds(CODE_EXPIRE_SECONDS));
            entity.setUsed(0);
            resetCodeMapper.insert(entity);
            log.info("验证龙文已下发：学号={}（演示环境回显）", user.getAccount());
        } else {
            // 未登记：不下发真实验证龙文，回显一个随机数字（防枚举），且不落库
            code = String.format("%06d", RANDOM.nextInt(1_000_000));
            log.info("验证龙文下发请求命中未登记账号（已做统一返回，不泄露存在性）：{}", dto.getAccount());
        }

        resetIpCooldown.put(clientIp, now);
        if (resetIpCooldown.size() > 1024) {
            long expireBefore = now - CODE_COOLDOWN_SECONDS * 1000L;
            resetIpCooldown.values().removeIf(ts -> ts < expireBefore);
        }

        // 统一返回：无论账号是否存在，响应结构一致，杜绝"账号是否注册"的批量探测
        // 真实项目在这里接邮件 / 短信服务；演示环境直接回显给前端
        return new SendCodeVO(dto.getAccount(), code, CODE_EXPIRE_SECONDS);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReset(ResetConfirmDTO dto) {
        // 不泄露账号是否存在：账号不存在时，与"验证码错误"返回同样的通用提示
        DrUser user = resolveUser(dto.getAccount());
        if (user == null) {
            throw new BizException("验证龙文错误或已过期，请重新获取");
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
