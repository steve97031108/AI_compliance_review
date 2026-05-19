package com.scu.aicompliance.service;

import com.scu.aicompliance.model.ChatMessage;
import com.scu.aicompliance.model.ComplianceResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 会话级别的聊天服务，每个用户会话维护独立的对话历史。
 * <p>
 * 使用 scoped proxy 允许从后台线程（如 AI 异步调用）安全访问 session 作用域的 bean。
 * 对话历史在每次变更时自动持久化到 ConversationStore（按用户分离存储）。
 * </p>
 */
@Service
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ConversationStore conversationStore;
    private final String username;

    /** 当前会话的对话历史消息列表 */
    private final List<ChatMessage> history;

    /** 历史是否已从文件加载 */
    private boolean loaded = false;

    public ChatService(ConversationStore conversationStore) {
        this.conversationStore = conversationStore;
        // 从 SecurityContextHolder 获取当前登录用户名
        this.username = getCurrentUsername();
        // 先初始化空列表，首次访问时懒加载
        this.history = new ArrayList<>();
    }

    /**
     * 确保历史已从文件加载（懒加载）。
     */
    private void ensureLoaded() {
        if (!loaded) {
            List<ChatMessage> saved = conversationStore.loadHistory(username);
            history.addAll(saved);
            loaded = true;
            log.info("已从文件加载用户 {} 的对话历史，共 {} 条消息", username, history.size());
        }
    }

    /**
     * 添加用户消息并持久化。
     */
    public void addUserMessage(String content) {
        ensureLoaded();
        ChatMessage msg = new ChatMessage("user", content, LocalDateTime.now());
        history.add(msg);
        persist(msg);
    }

    /**
     * 添加 AI 回复并持久化。
     */
    public void addAssistantMessage(String content) {
        ensureLoaded();
        ChatMessage msg = new ChatMessage("assistant", content, LocalDateTime.now());
        history.add(msg);
        persist(msg);
    }

    /**
     * 记录一条被合规审查拦截的用户消息并持久化。
     */
    public void addBlockedUserMessage(String content, ComplianceResult result) {
        ensureLoaded();
        ChatMessage msg = new ChatMessage("user", content, LocalDateTime.now(), result);
        history.add(msg);
        persist(msg);
    }

    /**
     * 记录一条被合规审查拦截的 AI 回复并持久化。
     */
    public void addBlockedAssistantMessage(String content, ComplianceResult result) {
        ensureLoaded();
        ChatMessage msg = new ChatMessage("assistant", content, LocalDateTime.now(), result);
        history.add(msg);
        persist(msg);
    }

    /**
     * 清空当前用户的对话历史（内存 + 文件）。
     */
    public void clearHistory() {
        ensureLoaded();
        history.clear();
        conversationStore.clearHistory(username);
        log.info("已清空用户 {} 的对话历史", username);
    }

    /**
     * 获取完整的对话历史列表。
     *
     * @return 对话历史消息列表（返回内部列表引用，调用方可直接操作）
     */
    public List<ChatMessage> getHistory() {
        ensureLoaded();
        return history;
    }

    /**
     * 追加单条消息到持久化文件。
     */
    private void persist(ChatMessage msg) {
        try {
            conversationStore.appendMessage(username, msg);
        } catch (Exception e) {
            log.error("持久化用户 {} 的对话消息失败", username, e);
        }
    }

    /**
     * 从 Spring Security 上下文获取当前登录用户名。
     */
    private static String getCurrentUsername() {
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return "anonymous";
    }
}