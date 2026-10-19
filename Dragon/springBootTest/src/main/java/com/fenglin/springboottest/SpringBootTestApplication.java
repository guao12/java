package com.fenglin.springboottest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

/**
 * 启动类。
 *
 * <p>Mapper 扫描已统一放在 {@code config/MybatisPlusConfig} 上的 @MapperScan，
 * 此处保持简洁。</p>
 */
@SpringBootApplication
public class SpringBootTestApplication {

	private static final Logger log = LoggerFactory.getLogger(SpringBootTestApplication.class);

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(SpringBootTestApplication.class, args);
		Environment env = context.getEnvironment();
		String port = env.getProperty("server.port", "8080");
		String profiles = String.join(",", env.getActiveProfiles());
		log.info("""
				
				======================================================
				  假面骑士创骑 · 骑士系统后端启动成功
				  环境 profile : {}
				  接口地址     : http://localhost:{}/api
				  健康检查     : http://localhost:{}/api/health
				  接口首页     : http://localhost:{}/index.html
				======================================================
				""", profiles.isEmpty() ? "default" : profiles, port, port, port);
	}

}
