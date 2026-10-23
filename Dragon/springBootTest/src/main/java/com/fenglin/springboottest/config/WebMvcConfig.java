package com.fenglin.springboottest.config;

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
 * @author fenglin
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final OptionalAuthInterceptor optionalAuthInterceptor;
    private final AdminInterceptor adminInterceptor;

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
                .allowedOriginPatterns("http://localhost:[*]", "http://127.0.0.1:[*]")
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

        // 首页展示与档案检索对所有人开放，但要识别登录者身份
        // 以便按在校身份决定可见密级（软鉴权：不带令牌也能访问，只是只能看公开卷宗）
        registry.addInterceptor(optionalAuthInterceptor)
                .addPathPatterns("/api/home/**", "/api/search", "/api/search/**", "/api/archive/**");

        // 后台管理系统：除登录接口外全部需要管理员令牌
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/auth/**");
    }
}
