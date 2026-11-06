package com.fenglin.springboottest.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置：跨域 + JWT 拦截器。
 *
 * <p>开发时前端 5177 端口通过 Vite 代理访问后端，其实不会触发跨域；
 * 这里仍然放开，方便用 Postman / 浏览器直接调试，或前端不用代理时直连。</p>
 *
 * <p>允许的来源通过 {@code dr.cors.allowed-origin-patterns} 配置（逗号分隔），
 * 默认放开 localhost / 127.0.0.1 任意端口；局域网部署时改为实际 IP，例如
 * {@code dr.cors.allowed-origin-patterns=http://192.168.1.100:[*]}。</p>
 *
 * @author fenglin
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final OptionalAuthInterceptor optionalAuthInterceptor;
    private final AdminInterceptor adminInterceptor;

    /** 允许的跨域来源（逗号分隔），默认 localhost / 127.0.0.1 任意端口 */
    @Value("${dr.cors.allowed-origin-patterns:http://localhost:[*],http://127.0.0.1:[*]}")
    private String allowedOriginPatterns;

    public WebMvcConfig(JwtInterceptor jwtInterceptor,
                        OptionalAuthInterceptor optionalAuthInterceptor,
                        AdminInterceptor adminInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
        this.optionalAuthInterceptor = optionalAuthInterceptor;
        this.adminInterceptor = adminInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                // allowedOriginPatterns 支持通配，且能与 allowCredentials(true) 共用
                .allowedOriginPatterns(allowedOriginPatterns.split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 学院内网接口需要 JWT；/api/auth/** 与 /api/health 放行
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/user/**");

        // 首页展示、档案检索与论坛读取对所有人开放，但要识别登录者身份
        // 以便按在校身份决定可见密级（软鉴权：不带令牌也能访问，只是只能看公开卷宗）
        // 论坛的写接口（发帖 / 回帖 / 点赞）也在同一路径下：这里只负责识别身份，
        // 是否登录由 ForumServiceImpl 判定，未登录返回 code=401，前端会跳登录页。
        registry.addInterceptor(optionalAuthInterceptor)
                .addPathPatterns("/api/home/**", "/api/search", "/api/search/**",
                        "/api/archive/**", "/api/forum/**");

        // 后台管理系统：除登录接口外全部需要管理员令牌
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/auth/login");
    }
}
