package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fenglin.springboottest.common.BizException;
import com.fenglin.springboottest.dto.AdminLoginDTO;
import com.fenglin.springboottest.dto.SpiritSaveDTO;
import com.fenglin.springboottest.entity.DrAdmin;
import com.fenglin.springboottest.entity.DrSpirit;
import com.fenglin.springboottest.mapper.DrAdminMapper;
import com.fenglin.springboottest.mapper.DrSpiritMapper;
import com.fenglin.springboottest.service.AdminService;
import com.fenglin.springboottest.service.TokenService;
import com.fenglin.springboottest.vo.AdminLoginVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 后台管理系统服务实现。
 *
 * @author fenglin
 */
@Service
public class AdminServiceImpl implements AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminServiceImpl.class);

    /** 言灵密级（与首页展示约定一致） */
    private static final List<String> SPIRIT_LEVELS = List.of("内部", "机密", "高危", "绝密");

    /** 后台登录失败锁定：5 次 / 15 分钟（后台权限高于学员端，必须防爆破） */
    private static final int LOGIN_MAX_FAIL = 5;
    private static final long LOGIN_LOCK_MS = 15 * 60 * 1000L;

    /** username(小写) -> {失败次数, 最近失败时间} */
    private final Map<String, long[]> loginFailures = new ConcurrentHashMap<>();

    private final DrAdminMapper adminMapper;
    private final DrSpiritMapper spiritMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AdminServiceImpl(DrAdminMapper adminMapper,
                            DrSpiritMapper spiritMapper,
                            PasswordEncoder passwordEncoder,
                            TokenService tokenService) {
        this.adminMapper = adminMapper;
        this.spiritMapper = spiritMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    /* ================= 管理员登录 ================= */

    @Override
    public AdminLoginVO login(AdminLoginDTO dto) {
        String lockKey = dto.getUsername() == null ? "" : dto.getUsername().trim().toLowerCase(Locale.ROOT);
        if (isLoginLocked(lockKey)) {
            throw new BizException("尝试过于频繁，请 15 分钟后再试");
        }

        DrAdmin admin = adminMapper.selectOne(
                new LambdaQueryWrapper<DrAdmin>().eq(DrAdmin::getUsername, dto.getUsername()));
        if (admin == null || !passwordEncoder.matches(dto.getPassword(), admin.getPassword())) {
            // 账号不存在与密码错误给同一句话，不向探测者泄露账号是否存在
            recordLoginFailure(lockKey);
            throw new BizException("管理员账号或密码不正确");
        }
        if (admin.getStatus() == null || admin.getStatus() == 0) {
            throw new BizException("该管理员账号已被封禁，请联系学院负责人");
        }
        clearLoginFailure(lockKey);

        boolean remember = Boolean.TRUE.equals(dto.getRemember());
        String token = tokenService.issueAdmin(admin.getId(), admin.getUsername(), remember);

        // 登录计数 + 最近登录时间（失败不影响主流程）
        try {
            adminMapper.update(null, new LambdaUpdateWrapper<DrAdmin>()
                    .eq(DrAdmin::getId, admin.getId())
                    .set(DrAdmin::getLoginCount, (admin.getLoginCount() == null ? 0 : admin.getLoginCount()) + 1)
                    .set(DrAdmin::getLastLoginAt, LocalDateTime.now()));
        } catch (Exception e) {
            log.warn("更新管理员登录信息失败：{}", e.getMessage());
        }

        AdminLoginVO vo = new AdminLoginVO();
        vo.setToken(token);
        vo.setExpiresIn(tokenService.expiresIn(remember));
        vo.setAdmin(new AdminLoginVO.AdminInfo(admin.getId(), admin.getUsername(), admin.getNickname()));
        log.info("后台管理员登录：{}", admin);
        return vo;
    }

    @Override
    public DrAdmin getAdminById(Long id) {
        return adminMapper.selectById(id);
    }

    /* ================= 言灵图鉴管理 ================= */

    @Override
    public List<DrSpirit> listSpirits() {
        return spiritMapper.selectList(
                new LambdaQueryWrapper<DrSpirit>().orderByAsc(DrSpirit::getSort).orderByAsc(DrSpirit::getId));
    }

    @Override
    public List<DrSpirit> publicSpirits() {
        return spiritMapper.selectList(
                new LambdaQueryWrapper<DrSpirit>()
                        .eq(DrSpirit::getStatus, 1)
                        .orderByAsc(DrSpirit::getSort)
                        .orderByAsc(DrSpirit::getId));
    }

    @Override
    public DrSpirit createSpirit(SpiritSaveDTO dto) {
        checkLevel(dto.getLevel());
        Long exists = spiritMapper.selectCount(
                new LambdaQueryWrapper<DrSpirit>().eq(DrSpirit::getSpiritKey, dto.getKey()));
        if (exists != null && exists > 0) {
            throw new BizException("言灵 key「" + dto.getKey() + "」已存在，请换一个");
        }

        DrSpirit spirit = new DrSpirit();
        applyFields(spirit, dto);
        try {
            spiritMapper.insert(spirit);
        } catch (DuplicateKeyException e) {
            // 逻辑删除的条目仍然占着 uk_spirit_key 唯一索引，而上面的 selectCount 会被
            // MyBatis-Plus 自动追加 deleted=0，看不到这些"墓碑"行，所以只能靠数据库兜底。
            throw new BizException("言灵 key「" + dto.getKey()
                    + "」已被占用（可能来自已删除的条目），请换一个 key");
        }
        log.info("新增言灵：{}", spirit);
        return spirit;
    }

    @Override
    public DrSpirit updateSpirit(Long id, SpiritSaveDTO dto) {
        checkLevel(dto.getLevel());
        DrSpirit spirit = spiritMapper.selectById(id);
        if (spirit == null) {
            throw new BizException("该言灵条目不存在或已被删除");
        }
        // key 改动时检查唯一性
        if (!spirit.getSpiritKey().equals(dto.getKey())) {
            Long exists = spiritMapper.selectCount(
                    new LambdaQueryWrapper<DrSpirit>().eq(DrSpirit::getSpiritKey, dto.getKey()));
            if (exists != null && exists > 0) {
                throw new BizException("言灵 key「" + dto.getKey() + "」已被其他条目占用");
            }
        }

        applyFields(spirit, dto);
        try {
            spiritMapper.updateById(spirit);
        } catch (DuplicateKeyException e) {
            // 两条记录互换 key 的场景：查重查不到冲突，最终由唯一索引拦下
            throw new BizException("言灵 key「" + dto.getKey() + "」已被其他条目占用");
        }
        log.info("编辑言灵：{}", spirit);
        return spirit;
    }

    @Override
    public void deleteSpirit(Long id) {
        DrSpirit spirit = spiritMapper.selectById(id);
        if (spirit == null) {
            throw new BizException("该言灵条目不存在或已被删除");
        }
        spiritMapper.deleteById(id);
        log.info("删除言灵：id={} key={}", id, spirit.getSpiritKey());
    }

    /* ================= 内部方法 ================= */

    private boolean isLoginLocked(String key) {
        long[] rec = loginFailures.get(key);
        if (rec == null) {
            return false;
        }
        if (System.currentTimeMillis() - rec[1] > LOGIN_LOCK_MS) {
            loginFailures.remove(key);
            return false;
        }
        return rec[0] >= LOGIN_MAX_FAIL;
    }

    private void recordLoginFailure(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        long now = System.currentTimeMillis();
        loginFailures.compute(key, (k, v) -> {
            if (v == null || now - v[1] > LOGIN_LOCK_MS) {
                return new long[]{1, now};
            }
            v[0] += 1;
            v[1] = now;
            return v;
        });
        // 惰性清理，避免内存无限增长
        if (loginFailures.size() > 1024) {
            loginFailures.entrySet().removeIf(e -> now - e.getValue()[1] > LOGIN_LOCK_MS);
        }
    }

    private void clearLoginFailure(String key) {
        if (key != null) {
            loginFailures.remove(key);
        }
    }

    private void checkLevel(String level) {
        if (!SPIRIT_LEVELS.contains(level)) {
            throw new BizException("密级只能是：" + String.join(" / ", SPIRIT_LEVELS));
        }
    }

    /** 把表单字段拷贝到实体（编辑时前端可改 key，但路由插画需对应 public/img/spirit/<key>.webp） */
    private void applyFields(DrSpirit spirit, SpiritSaveDTO dto) {
        spirit.setSpiritKey(dto.getKey());
        spirit.setName(dto.getName());
        spirit.setElement(dto.getElement());
        spirit.setLevel(dto.getLevel());
        spirit.setEffect(dto.getEffect());
        spirit.setIntro(dto.getDesc());
        spirit.setHolder(dto.getHolder() == null ? "" : dto.getHolder());
        spirit.setNote(dto.getNote() == null ? "" : dto.getNote());
        spirit.setImageUrl(dto.getImageUrl() == null ? "" : dto.getImageUrl().trim());
        spirit.setSort(dto.getSort() == null ? 0 : dto.getSort());
        spirit.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
    }
}
