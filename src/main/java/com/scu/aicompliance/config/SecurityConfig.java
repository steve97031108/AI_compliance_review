package com.scu.aicompliance.config;

import com.scu.aicompliance.service.UserService;
import com.scu.aicompliance.ui.LoginView;
import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(jsr250Enabled = true)
public class SecurityConfig extends VaadinWebSecurity {

    private final UserService userService;

    public SecurityConfig(UserService userService) {
        this.userService = userService;
    }

    /**
     * 密码编码器 — 返回 BCryptPasswordEncoder 具体类型，
     * 确保 UserService 能通过构造器注入该 Bean。
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * UserDetailsService — 从 users.json 加载用户数据。
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userService.findByUsername(username)
                .map(user -> org.springframework.security.core.userdetails.User
                        .withUsername(user.getUsername())
                        .password(user.getPasswordHash())
                        .roles(user.getRole())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));
    }

    /**
     * 核心安全配置 — 使用 VaadinWebSecurity 基类，
     * 自动处理 @AnonymousAllowed / @PermitAll 注解并配置 CSRF 忽略。
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // 先设登录视图，再调用父类配置（避免 anyRequest 后添加 matchers 报错）
        setLoginView(http, LoginView.class);

        // Vaadin 默认安全配置（CSRF 令牌忽略、公共资源放行等），
        // SettingsView 已通过 @RolesAllowed("ADMIN") 注解保护。
        super.configure(http);
    }
}
