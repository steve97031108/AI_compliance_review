package com.scu.aicompliance.config;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.PWA;
import org.springframework.context.annotation.Configuration;

/**
 * Vaadin 24 全局应用外壳配置。
 * @Push 必须定义在此处，不能直接放在 UI 类上。
 */
@Configuration
@Push
@PWA(name = "AI 合规审查系统", shortName = "合规审查")
public class AppShellConfig implements AppShellConfigurator {
}