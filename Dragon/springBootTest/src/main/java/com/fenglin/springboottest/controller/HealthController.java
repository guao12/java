package com.fenglin.springboottest.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fenglin.springboottest.common.R;
import com.fenglin.springboottest.entity.DrUser;
import com.fenglin.springboottest.mapper.DrUserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查接口，前端登录页用它显示「后端通道 · 已连接」。
 *
 * @author fenglin
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final DrUserMapper userMapper;
    private final Environment environment;
    private final DataSource dataSource;

    @Value("${spring.application.name:springBootTest}")
    private String appName;

    public HealthController(DrUserMapper userMapper, Environment environment, DataSource dataSource) {
        this.userMapper = userMapper;
        this.environment = environment;
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public R<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("app", appName);

        String[] profiles = environment.getActiveProfiles();
        data.put("profile", profiles.length == 0 ? "default" : String.join(",", profiles));
        data.put("port", environment.getProperty("server.port"));

        // 真正落一次库，确认 MySQL + MyBatis-Plus 都通了
        Long userCount = userMapper.selectCount(new QueryWrapper<DrUser>());
        data.put("database", describeDatabase());
        data.put("userCount", userCount);
        data.put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        return R.ok(data);
    }

    /**
     * 返回真实连接到的库名。
     *
     * <p>以前这里写死成「MySQL 已连接（dragon_raja）」，跑 test profile（dragon_raja_test）
     * 或临时用 DR_DB_URL 指向别的库时，健康检查会给出错误结论，排查时容易被误导。
     * 现在改成从连接元信息里取当前 catalog，取不到时降级为不带库名的文案。</p>
     */
    private String describeDatabase() {
        try (Connection conn = dataSource.getConnection()) {
            String catalog = conn.getCatalog();
            return (catalog == null || catalog.isBlank())
                    ? "MySQL 已连接"
                    : "MySQL 已连接（" + catalog + "）";
        } catch (SQLException e) {
            return "MySQL 已连接（库名获取失败：" + e.getMessage() + "）";
        }
    }
}
