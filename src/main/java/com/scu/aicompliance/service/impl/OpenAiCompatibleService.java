package com.scu.aicompliance.service.impl;

import com.scu.aicompliance.model.ChatMessage;
import com.scu.aicompliance.model.SystemConfig;
import com.scu.aicompliance.service.AiService;
import com.scu.aicompliance.service.ConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * OpenAI 兼容接口实现 — 支持任意兼容 OpenAI API 格式的大语言模型。
 */
@Service
@RequiredArgsConstructor
public class OpenAiCompatibleService implements AiService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleService.class);

    private final ConfigService configService;

    @Override
    public String generateResponse(List<ChatMessage> messages) {
        SystemConfig config = configService.getConfig();

        // 检查 API 密钥是否已配置
        if (config.getOpenaiApiKey() == null || config.getOpenaiApiKey().isBlank()) {
            return "⚠️ 系统未配置API密钥，请联系管理员在设置页面填写";
        }

        try {
            // 构建 OpenAI 客户端（支持自定义 baseUrl）
            OpenAiService openAiService = buildOpenAiService(config);

            // 将自定义 ChatMessage 转换为 OpenAI 客户端 ChatMessage
            List<com.theokanning.openai.completion.chat.ChatMessage> openAiMessages =
                    messages.stream()
                            .map(msg -> new com.theokanning.openai.completion.chat.ChatMessage(
                                    msg.getRole(),
                                    msg.getContent()))
                            .collect(Collectors.toList());

            // 构建请求
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(config.getModelName())
                    .messages(openAiMessages)
                    .temperature(config.getTemperature())
                    .maxTokens(config.getMaxTokens())
                    .build();

            log.info("调用 AI API: model={}, messages={}", config.getModelName(), openAiMessages.size());

            // 调用 API
            ChatCompletionResult result = openAiService.createChatCompletion(request);

            // 提取生成的文本
            String reply = result.getChoices().get(0).getMessage().getContent();
            log.info("AI 回复: {} chars", reply.length());
            return reply;

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
     * 根据系统配置构建 OpenAI 客户端实例。
     * 通过 Retrofit 手动构建 OpenAiApi 以支持自定义 baseUrl。
     */
    private OpenAiService buildOpenAiService(SystemConfig config) {
        String token = config.getOpenaiApiKey();
        Duration timeout = Duration.ofSeconds(
                config.getTimeoutSeconds() != null ? config.getTimeoutSeconds() : 60);
        String baseUrl = config.getOpenaiBaseUrl();

        // 自定义 baseUrl 时通过 Retrofit 构建 OpenAiApi
        if (baseUrl != null && !baseUrl.isBlank()) {
            ObjectMapper mapper = com.theokanning.openai.service.OpenAiService.defaultObjectMapper();
            OkHttpClient client = com.theokanning.openai.service.OpenAiService.defaultClient(token, timeout)
                    .newBuilder()
                    .connectTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                    .readTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                    .build();
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl.endsWith("/") ? baseUrl : baseUrl + "/")
                    .client(client)
                    .addConverterFactory(JacksonConverterFactory.create(mapper))
                    .build();
            OpenAiApi api = retrofit.create(OpenAiApi.class);
            return new OpenAiService(api);
        }
        return new OpenAiService(token, timeout);
    }
}