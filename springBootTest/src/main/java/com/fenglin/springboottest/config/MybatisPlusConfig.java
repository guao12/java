package com.fenglin.springboottest.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置。
 *
 * <p>说明：分页插件（PaginationInnerInterceptor）自 MyBatis-Plus 3.5.9 起被拆分到
 * 独立的 {@code mybatis-plus-jsqlparser} 依赖中，本项目暂未使用分页，
 * 因此这里只做 Mapper 包扫描。需要分页时按下述三步补齐：</p>
 * <pre>
 * 1) pom.xml 增加依赖 com.baomidou:mybatis-plus-jsqlparser:${mybatis-plus.version}
 * 2) 在下方注册 MybatisPlusInterceptor Bean
 * 3) 调用 page(new Page&lt;&gt;(current, size), wrapper)
 * </pre>
 *
 * @author fenglin
 */
@Configuration
@MapperScan("com.fenglin.springboottest.mapper")
public class MybatisPlusConfig {
}
