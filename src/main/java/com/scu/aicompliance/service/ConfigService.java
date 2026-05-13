package com.scu.aicompliance.service;

import com.scu.aicompliance.model.SystemConfig;
import com.scu.aicompliance.util.JsonUtil;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;

/**
 * 系统配置服务 — 从 config.json 加载 API 密钥、模型参数等。
 */
@Service
public class ConfigService {

    private static final Logger log = LoggerFactory.getLogger(ConfigService.class);
    private static final Path CONFIG_PATH = Path.of("src", "main", "resources", "config.json");

    private SystemConfig config;

    @PostConstruct
    public void init() {
        File file = CONFIG_PATH.toFile();
        if (file.exists()) {
            config = JsonUtil.readValue(file, SystemConfig.class);
            log.info("已加载系统配置: model={}, baseUrl={}", config.getModelName(), config.getOpenaiBaseUrl());
        } else {
            log.warn("config.json 不存在，使用默认空配置。请创建 src/main/resources/config.json 填入 API 密钥。");
            config = new SystemConfig();
        }
    }

    public SystemConfig getConfig() {
        return config;
    }

    public void saveConfig(SystemConfig newConfig) {
        this.config = newConfig;
        JsonUtil.writeValue(CONFIG_PATH.toFile(), newConfig);
        log.info("系统配置已保存");
    }
}