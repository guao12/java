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

    @Value("${spring.application.name:springBootTest}")
    private String appName;

    public HealthController(DrUserMapper userMapper, Environment environment) {
        this.userMapper = userMapper;
        this.environment = environment;
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
        data.put("database", "MySQL 已连接（dragon_raja）");
        data.put("userCount", userCount);
        data.put("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        return R.ok(data);
    }
}
