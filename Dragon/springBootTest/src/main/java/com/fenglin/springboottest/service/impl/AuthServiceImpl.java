package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fenglin.springboottest.common.BizException;
import com.fenglin.springboottest.dto.LoginDTO;
import com.fenglin.springboottest.dto.RegisterDTO;
import com.fenglin.springboottest.dto.RegisterSendDTO;
import com.fenglin.springboottest.dto.ResetConfirmDTO;
import com.fenglin.springboottest.dto.ResetSendDTO;
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
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.Arrays;
import java.util.Map;
import java.util.Locale;
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

    private static final SecureRandom RANDOM = new SecureRandom();

    /** 验证龙文下发通道：true=随响应回显（仅本地演示），false=只写日志 */
    private final boolean resetCodeEchoRequested;

    /** 实际生效的回显开关：生产环境强制关闭 */
    private boolean resetCodeEcho = false;

    private final DrUserMapper userMapper;
    private final DrResetCodeMapper resetCodeMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final DictService dictService;
    private final CaptchaService captchaService;
    private final Environment environment;

    /** 验证龙文下发冷却：按客户端真实 IP 限流（防脚本批量探测 / 刷取） */
    private final Map<String, Long> resetIpCooldown = new ConcurrentHashMap<>();

    /** 登录失败计数：account -> 累计失败次数（含时间戳用于过期清理），防暴力破解 */
    private static final int LOGIN_MAX_FAIL = 5;
    private static final long LOGIN_LOCK_MS = 15 * 60 * 1000L;
    private static final long LOGIN_FAIL_WINDOW_MS = 15 * 60 * 1000L;
    private final Map<String, long[]> loginFailures = new ConcurrentHashMap<>();

    /** 重置验证龙文尝试计数：account -> 累计输错次数，防止 6 位验证码被穷举 */
    private static final int RESET_MAX_FAIL = 5;
    private static final long RESET_LOCK_MS = 5 * 60 * 1000L;
    private final Map<String, long[]> resetFailures = new ConcurrentHashMap<>();

    public AuthServiceImpl(DrUserMapper userMapper,
                           DrResetCodeMapper resetCodeMapper,
                           PasswordEncoder passwordEncoder,
                           TokenService tokenService,
                           DictService dictService,
                           CaptchaService captchaService,
                           Environment environment,
                           @Value("${dr.auth.reset-code-echo:false}") boolean resetCodeEchoRequested) {
        this.userMapper = userMapper;
        this.resetCodeMapper = resetCodeMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.dictService = dictService;
        this.captchaService = captchaService;
        this.environment = environment;
        this.resetCodeEchoRequested = resetCodeEchoRequested;
    }

    /**
     * 确定验证龙文下发通道。
     *
     * <p>回显模式（把验证码直接放在响应里）等价于「谁都能重置别人的密钥」，
     * 只允许在本地演示环境打开；只要激活了 prod 环境，无论配置怎么写都强制关闭。</p>
     */
    @PostConstruct
    void initResetCodeChannel() {
        boolean prod = Arrays.stream(environment.getActiveProfiles()).anyMatch("prod"::equalsIgnoreCase);
        this.resetCodeEcho = resetCodeEchoRequested && !prod;
        if (resetCodeEchoRequested && prod) {
            log.error("生产环境配置了 dr.auth.reset-code-echo=true，已强制关闭：回显验证码等同于任意账号可被接管");
        }
        if (!this.resetCodeEcho) {
            log.warn("验证龙文通道 = log：验证码仅写入日志，用户收不到。"
                    + "上线前请实现真实下发（短信 / 邮件 / 站内信），或在 dev 环境用 dr.auth.reset-code-echo=true 走演示回显");
        }
    }

    // ------------------------------------------------------------
    // 注册
    // ------------------------------------------------------------

    /**
     * 图形验证码校验（注册前置）。
     * <p>注册已改为「图形验证码即唯一人机校验」，不再下发 6 位数字验证码；
     * 本方法只负责消费一次性凭证，供前端在进入下一步前做一次校验，
     * 真正的注册入库仍由 {@link #register(RegisterDTO)} 再校验一次。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegisterCodeVO sendRegisterCode(RegisterSendDTO dto, String clientIp) {
        // 1. 服务端 IP 限流：同一真实 IP 60 秒内只能校验一次，防脚本批量刷取
        long now = System.currentTimeMillis();
        Long last = resetIpCooldown.get("register:" + clientIp);
        if (last != null && now - last < CODE_COOLDOWN_SECONDS * 1000L) {
            throw new BizException("验证码校验过于频繁，请 1 分钟后再试");
        }

        // 2. 图形验证码：命中即从服务端删除令牌，保证一次性使用
        if (!captchaService.verify(dto.getCaptchaToken(), dto.getCaptchaCode())) {
            throw new BizException("图形验证码错误或已过期，请点击图片刷新后重试");
        }

        // 3. 限流时间戳落盘（带惰性清理，避免内存无限增长）
        resetIpCooldown.put("register:" + clientIp, now);
        if (resetIpCooldown.size() > 1024) {
            long expireBefore = now - CODE_COOLDOWN_SECONDS * 1000L;
            resetIpCooldown.values().removeIf(ts -> ts < expireBefore);
        }

        log.info("【注册验证码】图形验证码校验通过 ip={} clientKey={}", clientIp, dto.getClientKey());
        // 不再回显 6 位数字验证码：expireSeconds 仅用于前端提示有效期
        return new RegisterCodeVO(null, CODE_EXPIRE_SECONDS, CODE_COOLDOWN_SECONDS);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegisterVO register(RegisterDTO dto) {
        String codename = dto.getCodename().trim();

        // 0. 图形验证码校验（注册唯一的人机校验：不再下发 6 位数字验证码）
        //    verify 命中即从服务端内存删除令牌，保证一次性使用，防止同一张图被重放批量注册
        if (!captchaService.verify(dto.getCaptchaToken(), dto.getCaptchaCode())) {
            throw new BizException("图形验证码错误或已过期，请点击图片刷新后重试");
        }

        // 1. 在校身份：安全修复 —— 注册一律强制为「新生 freshman」。
        //    原实现信任前端提交的 identity，攻击者自选 teacher/executor 即可越权查阅高密级档案。
        //    身份升级必须由后台管理员走「身份变更」流程，注册阶段不开放自选。
        String identity = "freshman";
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
        user.setIdentity(identity);
        user.setSpirit(spirit);
        user.setStatus(1);
        user.setLoginCount(0);

        // 4. 落库（codename 已加唯一索引；并发冲突由唯一键兜底）
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 区分撞的是「学号」还是「行动代号」：学号并发撞车应提示重试，而不是误导用户换代号
            String msg = e.getMessage() == null ? "" : e.getMessage();
            if (msg.contains("uk_account")) {
                throw new BizException("系统繁忙，请稍后重试（学号签发冲突）");
            }
            if (msg.contains("uk_codename") || msg.contains("codename")) {
                throw new BizException("行动代号「" + codename + "」已被使用，换一个吧");
            }
            // 兜底：索引名未命中（驱动 / 版本差异）时按代号冲突提示，至少不暴露 500
            log.warn("注册落库唯一键冲突（未识别索引名）：{}", msg);
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
    // 注册人机校验：图形验证码（原 6 位数字验证码下发流程已移除）
    // ------------------------------------------------------------

    // ------------------------------------------------------------
    // 登录
    // ------------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO login(LoginDTO dto) {
        String input = dto.getAccount().trim();

        // 1. 先解析用户：锁定键必须绑定到「真实用户」，而不是用户输入的字符串。
        //    否则同一账号可用「大小写变体」或「学号 / 姓名」两种写法绕开计数（库中比较大小写不敏感）。
        DrUser user = resolveUser(input);
        String lockKey = user != null ? "u:" + user.getId() : "i:" + lockKeyOf(input);

        // 0. 登录失败锁定：同一账号连续失败过多时临时锁定，防撞库 / 暴力破解
        if (isLoginLocked(lockKey)) {
            throw new BizException("尝试过于频繁，请 15 分钟后再试");
        }

        // 统一提示语，避免暴露"该账号是否存在"
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            recordLoginFailure(lockKey);
            throw new BizException("学号或言灵密钥不正确");
        }
        if (user.getStatus() == null || user.getStatus() == 0) {
            throw new BizException("该身份卡已被学院封印，请联系执行部");
        }
        clearLoginFailure(lockKey);

        // 2. 累计登录次数 + 最近登录时间
        userMapper.update(null, new LambdaUpdateWrapper<DrUser>()
                .eq(DrUser::getId, user.getId())
                .setSql("login_count = login_count + 1")
                .set(DrUser::getLastLoginAt, LocalDateTime.now()));
        user.setLoginCount((user.getLoginCount() == null ? 0 : user.getLoginCount()) + 1);
        user.setLastLoginAt(LocalDateTime.now());

        // 3. 签发 JWT（勾选"记住身份卡"时使用长令牌）
        boolean remember = Boolean.TRUE.equals(dto.getRemember());
        String token = tokenService.issue(user.getId(), user.getAccount(), remember, pwdVer(user.getPassword()));

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

        String echoedCode = null;
        DrUser user = resolveUser(dto.getAccount());
        if (user != null) {
            // 已登记：生成验证龙文并落库（5 分钟有效）
            String code = String.format("%06d", RANDOM.nextInt(1_000_000));
            DrResetCode entity = new DrResetCode();
            entity.setAccount(user.getAccount());
            entity.setCode(code);
            entity.setExpireTime(LocalDateTime.now().plusSeconds(CODE_EXPIRE_SECONDS));
            entity.setUsed(0);
            resetCodeMapper.insert(entity);
            if (resetCodeEcho) {
                echoedCode = code;
                log.warn("【重置验证龙文】学号={} code={}（dev 回显通道：已随响应返回给前端）", user.getAccount(), code);
            } else {
                log.info("【重置验证龙文】学号={} code={}（日志通道：用户无法收到，需接入短信 / 邮件下发）", user.getAccount(), code);
            }
        } else {
            // 未登记：不下发真实验证龙文、不落库，仅记录日志（防枚举）
            log.info("验证龙文下发请求命中未登记账号（已做统一返回，不泄露存在性）：{}", dto.getAccount());
            // 回显模式下也要回一个假码，否则「响应里有没有 demoCode」本身就成了账号存在性探针
            if (resetCodeEcho) {
                echoedCode = String.format("%06d", RANDOM.nextInt(1_000_000));
            }
        }

        resetIpCooldown.put(clientIp, now);
        if (resetIpCooldown.size() > 1024) {
            long expireBefore = now - CODE_COOLDOWN_SECONDS * 1000L;
            resetIpCooldown.values().removeIf(ts -> ts < expireBefore);
        }

        // 统一返回：无论账号是否存在，响应结构一致，杜绝"账号是否注册"的批量探测。
        // 是否带回验证码由下发通道决定（生产恒为 null，只走日志 / 真实下发）。
        return new SendCodeVO(dto.getAccount(), echoedCode, CODE_EXPIRE_SECONDS);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReset(ResetConfirmDTO dto) {
        // 不泄露账号是否存在：账号不存在时，与"验证码错误"返回同样的通用提示
        DrUser user = resolveUser(dto.getAccount());
        if (user == null) {
            throw new BizException("验证龙文错误或已过期，请重新获取");
        }

        // 0. 尝试次数限制：同一账号验证龙文连续输错过多时锁定，防 6 位验证码被穷举
        if (isResetLocked(user.getAccount())) {
            throw new BizException("验证龙文尝试过于频繁，请重新获取后再试");
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
            recordResetFailure(user.getAccount());
            throw new BizException("验证龙文错误或已过期，请重新获取");
        }
        clearResetFailure(user.getAccount());

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

    /**
     * 密码版本标识：取自 BCrypt 哈希的短摘要。
     * 改密后哈希变化 → 版本变化 → 携带旧版本的 JWT 自动失效（配合 TokenService#parseUserId(token, pwdVer)）。
     */
    public static String pwdVer(String bcryptHash) {
        if (bcryptHash == null || bcryptHash.length() < 16) {
            return "";
        }
        // 取 BCrypt 盐段（$2a$10$ 后 22 位）作为版本，任何密码变化都会改变盐与哈希
        return bcryptHash.substring(0, Math.min(bcryptHash.length(), 29));
    }

    // ------------------------------------------------------------
    // 登录失败锁定（内存计数，防暴力破解）
    // ------------------------------------------------------------

    /**
     * 未登记账号的锁定键：统一小写。
     * 数据库排序规则 utf8mb4_0900_ai_ci 大小写不敏感，
     * 若计数用原始输入，"ABC" 与 "abc" 会被当成两个账号，从而绕开锁定。
     */
    private static String lockKeyOf(String identifier) {
        return identifier == null ? "" : identifier.trim().toLowerCase(Locale.ROOT);
    }

    /** 账号是否处于锁定状态 */
    private boolean isLoginLocked(String account) {
        long[] rec = loginFailures.get(account);
        if (rec == null) {
            return false;
        }
        // 超过锁定窗口仍未解除，先清理再判断
        if (System.currentTimeMillis() - rec[1] > LOGIN_LOCK_MS) {
            loginFailures.remove(account);
            return false;
        }
        return rec[0] >= LOGIN_MAX_FAIL;
    }

    /** 记录一次登录失败；达到阈值后进入锁定窗口 */
    private void recordLoginFailure(String account) {
        if (account == null || account.isBlank()) {
            return;
        }        long now = System.currentTimeMillis();
        long[] rec = loginFailures.compute(account, (k, v) -> {
            if (v == null || now - v[1] > LOGIN_FAIL_WINDOW_MS) {
                return new long[]{1, now};
            }
            v[0] += 1;
            v[1] = now;
            return v;
        });
        // 惰性清理，避免内存无限增长
        if (loginFailures.size() > 4096) {
            loginFailures.entrySet().removeIf(e -> now - e.getValue()[1] > LOGIN_LOCK_MS);
        }
    }

    /** 登录成功后清除失败记录 */
    private void clearLoginFailure(String account) {
        if (account != null) {
            loginFailures.remove(account);
        }
    }

    // ------------------------------------------------------------
    // 重置验证龙文尝试限制
    // ------------------------------------------------------------

    private boolean isResetLocked(String account) {
        long[] rec = resetFailures.get(account);
        if (rec == null) {
            return false;
        }
        if (System.currentTimeMillis() - rec[1] > RESET_LOCK_MS) {
            resetFailures.remove(account);
            return false;
        }
        return rec[0] >= RESET_MAX_FAIL;
    }

    private void recordResetFailure(String account) {
        long now = System.currentTimeMillis();
        resetFailures.compute(account, (k, v) -> {
            if (v == null || now - v[1] > RESET_LOCK_MS) {
                return new long[]{1, now};
            }
            v[0] += 1;
            v[1] = now;
            return v;
        });
        if (resetFailures.size() > 4096) {
            resetFailures.entrySet().removeIf(e -> now - e.getValue()[1] > RESET_LOCK_MS);
        }
    }

    private void clearResetFailure(String account) {
        if (account != null) {
            resetFailures.remove(account);
        }
    }
}
