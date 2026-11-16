package com.fenglin.springboottest.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Web 配置：跨域 + JWT 拦截器。
 *
 * <p>开发时前端 5177 端口通过 Vite 代理访问后端，其实不会触发跨域；
 * 这里仍然放开，方便用 Postman / 浏览器直接调试，或前端不用代理时直连。</p>
 *
 * <p>允许的来源通过 {@code dr.cors.allowed-origin-patterns} 配置（逗号分隔），
 * 默认放开 192.168.68.1 / localhost / 127.0.0.1 任意端口；局域网部署时改为实际 IP，例如
 * {@code dr.cors.allowed-origin-patterns=http://192.168.1.100:[*]}。</p>
 *
 * @author fenglin
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final OptionalAuthInterceptor optionalAuthInterceptor;
    private final AdminInterceptor adminInterceptor;

    /** 允许的跨域来源（逗号分隔），默认本机 192.168.68.1 与 localhost / 127.0.0.1 任意端口 */
    @Value("${dr.cors.allowed-origin-patterns:http://192.168.68.1:[*],http://localhost:[*],http://127.0.0.1:[*]}")
    private String allowedOriginPatterns;

    /** 上传文件根目录（头像等），相对路径按启动目录解析 */
    @Value("${dr.upload.dir:uploads}")
    private String uploadDir;

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

    /**
     * 上传目录的静态映射：{@code /uploads/**} → {@code ${dr.upload.dir}}。
     *
     * <p>头像存的是站点相对路径（如 {@code /uploads/avatar/xxx.png}），数据库里
     * 只记这个路径，换域名 / 换端口都不用改数据。目录不存在时启动也不报错，
     * 第一次上传时由 AvatarStorage 创建。</p>
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        String location = dir.toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location)
                .setCachePeriod(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 学院内网接口需要 JWT；/api/auth/** 与 /api/health 放行
        // 好友与私信没有游客场景，一并走强制鉴权（未登录直接 401）
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/user/**", "/api/friend/**");

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
