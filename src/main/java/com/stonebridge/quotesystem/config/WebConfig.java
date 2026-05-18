package com.stonebridge.quotesystem.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 允许所有路径的请求
        registry.addMapping("/**")
                // 允许所有来源（开发环境下使用 allowedOriginPatterns("*") 最稳妥）
                .allowedOriginPatterns("*")
                // 重点：必须显式包含 DELETE 和 OPTIONS
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // 允许所有请求头
                .allowedHeaders("*")
                // 允许携带 Cookie
                .allowCredentials(true)
                // 预检请求的有效期（秒），1小时内同样的请求不再预检
                .maxAge(3600);
    }
}