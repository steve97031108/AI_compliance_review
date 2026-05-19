package com.scu.aicompliance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 应用基础配置 — 将 BCryptPasswordEncoder 独立出来，
 * 避免 UserService 与 SecurityConfig 之间的循环依赖。
 */
@Configuration
public class AppConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}