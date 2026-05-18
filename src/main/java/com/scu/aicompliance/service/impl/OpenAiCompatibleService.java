package com.scu.aicompliance.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scu.aicompliance.model.ChatMessage;
import com.scu.aicompliance.model.SystemConfig;
import com.scu.aicompliance.service.AiService;
import com.scu.aicompliance.service.ConfigService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * OpenAI 兼容接口实现 — 使用 Java 原生 HttpClient 直接调用 DeepSeek API。
 * <p>
 * DeepSeek API 端点：{@code https://api.deepseek.com/chat/completions}（无 /v1 前缀），
 * 与 OpenAI 标准库的 {@code /v1/chat/completions} 不同，因此不能直接使用 openai-gpt3-java 的 Retrofit 客户端。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class OpenAiCompatibleService implements AiService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * DeepSeek API Chat Completions 端点
     */
    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

    /** 系统提示词 — 通用 AI 对话模型。合规审查由上层 ComplianceService 独立执行。 */
    private static final String SYSTEM_PROMPT =
            "你是一个有用的AI对话模型。请根据用户的问题提供准确、有帮助的回复，使用中文。";

    private final ConfigService configService;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    @Override
    public String generateResponse(List<ChatMessage> messages) {
        SystemConfig config = configService.getConfig();

        // 检查 API 密钥是否已配置
        if (config.getOpenaiApiKey() == null || config.getOpenaiApiKey().isBlank()) {
            return "⚠️ 系统未配置API密钥，请联系管理员在设置页面填写";
        }

        try {
            // 构建请求体 JSON
            ObjectNode requestBody = MAPPER.createObjectNode();
            requestBody.put("model", config.getModelName());
            if (config.getTemperature() != null) {
                requestBody.put("temperature", config.getTemperature());
            }
            if (config.getMaxTokens() != null) {
                requestBody.put("max_tokens", config.getMaxTokens());
            }
            requestBody.put("stream", false);

            // messages: system prompt + 对话历史
            ArrayNode messagesArray = requestBody.putArray("messages");
            messagesArray.addObject()
                    .put("role", "system")
                    .put("content", SYSTEM_PROMPT);
            for (ChatMessage msg : messages) {
                messagesArray.addObject()
                        .put("role", msg.getRole())
                        .put("content", msg.getContent());
            }

            String jsonBody = MAPPER.writeValueAsString(requestBody);

            // 构建 API URL
            String baseUrl = config.getOpenaiBaseUrl();
            if (!baseUrl.endsWith("/")) {
                baseUrl += "/";
            }
            // 移除 baseUrl 中可能包含的尾部斜杠后再拼接，防止双斜杠
            String apiUrl = baseUrl.replaceAll("/+$", "") + CHAT_COMPLETIONS_PATH;

            log.info("调用 DeepSeek API: url={}, model={}, messages={}",
                    apiUrl, config.getModelName(), messagesArray.size());

            // 构建 HTTP 请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getOpenaiApiKey())
                    .timeout(Duration.ofSeconds(
                            config.getTimeoutSeconds() != null ? config.getTimeoutSeconds() : 60))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            // 发送请求
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            String responseBody = response.body();
            log.info("DeepSeek API 响应状态码: {}", response.statusCode());

            if (response.statusCode() != 200) {
                log.error("DeepSeek API 返回错误: status={}, body={}",
                        response.statusCode(), responseBody);
                return "❌ API调用失败（状态码：" + response.statusCode()
                        + "）：" + extractErrorMessage(responseBody);
            }

            // 解析响应（支持 content 和 reasoning_content 两种模式）
            ObjectNode responseJson = (ObjectNode) MAPPER.readTree(responseBody);
            String reply = responseJson
                    .path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText(null);
            // 推理模型（如 deepseek-v4-flash）将回复放在 reasoning_content
            if (reply == null || reply.isBlank()) {
                reply = responseJson
                        .path("choices")
                        .path(0)
                        .path("message")
                        .path("reasoning_content")
                        .asText("（AI 返回了空内容）");
            }

            log.info("AI 回复: {} chars", reply.length());
            return reply;

        } catch (java.net.http.HttpTimeoutException e) {
            log.error("AI 服务调用超时", e);
            return "❌ AI服务调用超时，请稍后重试";
        } catch (java.io.IOException e) {
            log.error("AI 服务网络异常", e);
            return "❌ 网络连接失败：" + e.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("AI 服务调用被中断", e);
            return "❌ 请求被中断";
        } catch (Exception e) {
            log.error("AI 服务调用失败", e);
            return "❌ AI服务调用失败：" + e.getMessage();
        }
    }

    @Override
    public String generateResponse(String prompt) {
        ChatMessage userMessage = new ChatMessage("user", prompt, null);
        return generateResponse(Collections.singletonList(userMessage));
    }

    /**
     * 从 DeepSeek 错误响应中提取可读的错误信息。
     */
    private String extractErrorMessage(String responseBody) {
        try {
            ObjectNode errorJson = (ObjectNode) MAPPER.readTree(responseBody);
            String message = errorJson.path("error").path("message").asText();
            return message != null && !message.isBlank() ? message : responseBody;
        } catch (Exception e) {
            // 截断过长的响应
            return responseBody.length() > 200
                    ? responseBody.substring(0, 200) + "..."
                    : responseBody;
        }
    }
}