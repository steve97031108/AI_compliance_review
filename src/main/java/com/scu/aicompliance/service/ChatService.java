package com.scu.aicompliance.service;

import com.scu.aicompliance.model.ChatMessage;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 会话级别的聊天服务，每个用户会话维护独立的对话历史。
 */
@Service
@SessionScope
public class ChatService {

    /** 当前会话的对话历史消息列表 */
    private final List<ChatMessage> history = new ArrayList<>();

    /**
     * 添加用户消息到对话历史。
     *
     * @param content 用户消息内容
     */
    public void addUserMessage(String content) {
        history.add(new ChatMessage("user", content, LocalDateTime.now()));
    }

    /**
     * 添加 AI 回复到对话历史。
     *
     * @param content AI 回复内容
     */
    public void addAssistantMessage(String content) {
        history.add(new ChatMessage("assistant", content, LocalDateTime.now()));
    }

    /**
     * 清空当前会话的对话历史。
     */
    public void clearHistory() {
        history.clear();
    }

    /**
     * 获取完整的对话历史列表。
     *
     * @return 对话历史消息列表（返回内部列表引用，调用方可直接操作）
     */
    public List<ChatMessage> getHistory() {
        return history;
    }
}