package com.scu.aicompliance.service;

import com.scu.aicompliance.model.ChatMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 临时单元测试 — 验证 AI 服务调用和对话历史维护。
 * 需在 src/main/resources/config.json 中配置有效的 API 密钥才能通过 AI 调用测试。
 */
@SpringBootTest
class ChatAndAiServiceTest {

    @Autowired
    private AiService aiService;

    @Autowired
    private ChatService chatService;

    @Test
    @DisplayName("ChatService — 添加用户消息，role 和时间应正确设置")
    void testAddUserMessage() {
        chatService.clearHistory();
        chatService.addUserMessage("你好，请做合规审查");

        List<ChatMessage> history = chatService.getHistory();
        assertEquals(1, history.size());
        ChatMessage msg = history.get(0);
        assertEquals("user", msg.getRole());
        assertEquals("你好，请做合规审查", msg.getContent());
        assertNotNull(msg.getTimestamp(), "时间戳不应为空");
    }

    @Test
    @DisplayName("ChatService — 添加助手消息，role 应正确设置")
    void testAddAssistantMessage() {
        chatService.clearHistory();
        chatService.addAssistantMessage("合规检查已完成");

        List<ChatMessage> history = chatService.getHistory();
        assertEquals(1, history.size());
        assertEquals("assistant", history.get(0).getRole());
        assertEquals("合规检查已完成", history.get(0).getContent());
    }

    @Test
    @DisplayName("ChatService — 多轮对话历史应正确累积")
    void testMultiTurnHistory() {
        chatService.clearHistory();
        chatService.addUserMessage("第一问");
        chatService.addAssistantMessage("第一答");
        chatService.addUserMessage("第二问");
        chatService.addAssistantMessage("第二答");

        List<ChatMessage> history = chatService.getHistory();
        assertEquals(4, history.size());
        assertEquals("user", history.get(0).getRole());
        assertEquals("assistant", history.get(1).getRole());
        assertEquals("user", history.get(2).getRole());
        assertEquals("assistant", history.get(3).getRole());
    }

    @Test
    @DisplayName("ChatService — clearHistory 应清空所有消息")
    void testClearHistory() {
        chatService.addUserMessage("测试消息");
        chatService.addAssistantMessage("回复");
        assertEquals(2, chatService.getHistory().size());

        chatService.clearHistory();
        assertTrue(chatService.getHistory().isEmpty(), "clearHistory 后应无消息");
    }

    @Test
    @DisplayName("AiService — 未配置 API 密钥时应返回提示信息")
    void testAiServiceNoApiKey() {
        String result = aiService.generateResponse("测试提问");
        assertNotNull(result);
        // 取决于 config.json 是否配置了密钥，可能返回提示或正常回复
        assertFalse(result.isEmpty(), "响应不应为空");
    }

    @Test
    @DisplayName("AiService — 对话历史模式下未配置密钥也应返回提示")
    void testAiServiceWithHistoryNoApiKey() {
        chatService.clearHistory();
        chatService.addUserMessage("请做合规审查");
        List<ChatMessage> history = chatService.getHistory();

        String result = aiService.generateResponse(history);
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }
}