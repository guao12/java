package com.fenglin.springboottest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 通用 Bean 配置。
 *
 * @author fenglin
 */
@Configuration
public class AppConfig {

    /**
     * 密码加密器。BCrypt 自带随机盐，同一个密码每次加密结果都不同，
     * 校验时用 {@code matches(明文, 密文)} 即可。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
