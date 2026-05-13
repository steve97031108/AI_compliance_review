package com.scu.aicompliance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class AiComplianceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiComplianceApplication.class, args);
    }

    /**
     * 将 BCryptPasswordEncoder Bean 放在主类中注册，
     * 避免 SecurityConfig → UserService → BCryptPasswordEncoder → SecurityConfig 的循环依赖。
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
