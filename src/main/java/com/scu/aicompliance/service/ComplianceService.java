package com.scu.aicompliance.service;

import com.scu.aicompliance.model.ComplianceResult;
import com.scu.aicompliance.model.SystemConfig;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 合规审查服务 — 调用 OpenAI 兼容 API 进行内容合规分析。
 */
@Service
public class ComplianceService {

    private static final Logger log = LoggerFactory.getLogger(ComplianceService.class);

    private static final String SYSTEM_PROMPT = """
            你是一个专业的内容合规审查助手。请审查用户提交的内容，判断是否违规。
            审查维度包括：色情低俗、暴力恐怖、违法犯罪、政治敏感、虚假信息、广告营销、侵犯隐私等。
            
            请以JSON格式返回审查结果，格式如下：
            {"violation": true或false, "reason": "违规原因简述", "riskLevel": "低风险/中风险/高风险"}
            
            只返回JSON，不要包含其他文字。""";

    private final ConfigService configService;

    public ComplianceService(ConfigService configService) {
        this.configService = configService;
    }

    /**
     * 审查文本内容是否合规。
     *
     * @param content 待审查的文本内容
     * @return ComplianceResult 审查结果
     */
    public ComplianceResult review(String content) {
        SystemConfig cfg = configService.getConfig();
        if (cfg.getOpenaiApiKey() == null || cfg.getOpenaiApiKey().isBlank()) {
            log.warn("未配置 API 密钥");
            return new ComplianceResult(false, "未配置 API 密钥，请先在系统设置中配置", "低风险");
        }

        try {
            OpenAiService service = new OpenAiService(
                    cfg.getOpenaiApiKey(),
                    Duration.ofSeconds(cfg.getTimeoutSeconds() != null ? cfg.getTimeoutSeconds() : 60)
            );

            // 如果有自定义 baseUrl，需要反射设置（openai-gpt3-java 支持）
            if (cfg.getOpenaiBaseUrl() != null && !cfg.getOpenaiBaseUrl().isBlank()) {
                setBaseUrl(service, cfg.getOpenaiBaseUrl());
            }

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(cfg.getModelName() != null ? cfg.getModelName() : "gpt-3.5-turbo")
                    .messages(List.of(
                            new ChatMessage("system", SYSTEM_PROMPT),
                            new ChatMessage("user", content)
                    ))
                    .temperature(cfg.getTemperature() != null ? cfg.getTemperature() : 0.7)
                    .maxTokens(cfg.getMaxTokens() != null ? cfg.getMaxTokens() : 512)
                    .build();

            ChatMessage response = service.createChatCompletion(request)
                    .getChoices().get(0).getMessage();

            String responseText = response.getContent().trim();
            // 清理可能的 markdown 代码块标记
            if (responseText.startsWith("```")) {
                responseText = responseText.replaceAll("```json\\s*", "").replaceAll("```\\s*$", "").trim();
            }

            log.info("AI 审查结果: {}", responseText);
            return parseResult(responseText);

        } catch (Exception e) {
            log.error("AI 审查请求失败", e);
            return new ComplianceResult(false, "审查服务异常: " + e.getMessage(), "低风险");
        }
    }

    private void setBaseUrl(OpenAiService service, String baseUrl) {
        try {
            var apiField = OpenAiService.class.getDeclaredField("api");
            apiField.setAccessible(true);
            var api = apiField.get(service);
            var baseUrlField = api.getClass().getDeclaredField("baseUrl");
            baseUrlField.setAccessible(true);
            baseUrlField.set(api, baseUrl.endsWith("/") ? baseUrl : baseUrl + "/");
        } catch (Exception e) {
            log.warn("无法设置自定义 baseUrl", e);
        }
    }

    private ComplianceResult parseResult(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, ComplianceResult.class);
        } catch (Exception e) {
            log.warn("解析AI返回结果失败，原始内容: {}", json);
            return new ComplianceResult(false, "无法解析审查结果: " + json, "低风险");
        }
    }
}