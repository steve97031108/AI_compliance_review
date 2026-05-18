package com.scu.aicompliance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scu.aicompliance.model.ComplianceResult;
import com.scu.aicompliance.model.SystemConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 合规审查服务 — 使用 Java 原生 HttpClient 直接调用 DeepSeek API 进行内容合规分析。
 */
@Service
public class ComplianceService {

    private static final Logger log = LoggerFactory.getLogger(ComplianceService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

    private static final String SYSTEM_PROMPT = """
            你是一个专业的内容合规审查助手。请审查用户提交的内容，判断是否违规。
            审查维度包括：色情低俗、暴力恐怖、违法犯罪、政治敏感、虚假信息、广告营销、侵犯隐私等。

            请以JSON格式返回审查结果，格式如下：
            {"violation": true或false, "reason": "违规原因简述", "riskLevel": "低风险/中风险/高风险"}

            只返回JSON，不要包含其他文字。""";

    private final ConfigService configService;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

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
            // 构建请求体
            ObjectNode requestBody = MAPPER.createObjectNode();
            requestBody.put("model",
                    cfg.getModelName() != null ? cfg.getModelName() : "deepseek-chat");
            requestBody.put("temperature",
                    cfg.getTemperature() != null ? cfg.getTemperature() : 0.7);
            requestBody.put("max_tokens",
                    cfg.getMaxTokens() != null ? cfg.getMaxTokens() : 512);
            requestBody.put("stream", false);

            ArrayNode messagesArray = requestBody.putArray("messages");
            messagesArray.addObject()
                    .put("role", "system")
                    .put("content", SYSTEM_PROMPT);
            messagesArray.addObject()
                    .put("role", "user")
                    .put("content", content);

            String jsonBody = MAPPER.writeValueAsString(requestBody);

            // 构建 API URL
            String baseUrl = cfg.getOpenaiBaseUrl();
            if (baseUrl == null || baseUrl.isBlank()) {
                baseUrl = "https://api.deepseek.com";
            }
            String apiUrl = baseUrl.replaceAll("/+$", "") + CHAT_COMPLETIONS_PATH;

            log.info("合规审查请求: url={}, model={}", apiUrl, cfg.getModelName());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + cfg.getOpenaiApiKey())
                    .timeout(Duration.ofSeconds(
                            cfg.getTimeoutSeconds() != null ? cfg.getTimeoutSeconds() : 60))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            String responseBody = response.body();

            if (response.statusCode() != 200) {
                log.error("合规审查 API 返回错误: status={}, body={}",
                        response.statusCode(), responseBody);
                return new ComplianceResult(false,
                        "审查服务异常 (HTTP " + response.statusCode() + ")", "低风险");
            }

            // 解析响应（支持 content 和 reasoning_content 两种模式）
            ObjectNode responseJson = (ObjectNode) MAPPER.readTree(responseBody);
            String responseText = responseJson
                    .path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText(null);
            if (responseText == null || responseText.isBlank()) {
                responseText = responseJson
                        .path("choices")
                        .path(0)
                        .path("message")
                        .path("reasoning_content")
                        .asText("")
                        .trim();
            }

            // 清理可能的 markdown 代码块标记
            if (responseText.startsWith("```")) {
                responseText = responseText
                        .replaceAll("```json\\s*", "")
                        .replaceAll("```\\s*$", "")
                        .trim();
            }

            log.info("合规审查结果: {}", responseText);
            return parseResult(responseText);

        } catch (Exception e) {
            log.error("合规审查请求失败", e);
            return new ComplianceResult(false, "审查服务异常: " + e.getMessage(), "低风险");
        }
    }

    private ComplianceResult parseResult(String json) {
        try {
            return MAPPER.readValue(json, ComplianceResult.class);
        } catch (Exception e) {
            log.warn("解析审查结果失败，原始内容: {}", json);
            return new ComplianceResult(false, "无法解析审查结果: " + json, "低风险");
        }
    }
}