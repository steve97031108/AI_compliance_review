package com.scu.aicompliance.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 聊天消息数据模型，用于存储对话中的单条消息。
 * <p>
 * 支持附带合规审查结果：
 * <ul>
 *   <li>{@code reviewResult == null} — 正常消息</li>
 *   <li>{@code role == "user" && reviewResult != null} — 用户输入被合规审查拦截</li>
 *   <li>{@code role == "assistant" && reviewResult != null} — AI 回复被合规审查拦截/替换</li>
 * </ul>
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    /** 角色：user / assistant / system */
    private String role;

    /** 消息内容 */
    private String content;

    /** 发送时间 */
    private LocalDateTime timestamp;

    /**
     * 合规审查结果（可选）。
     * <p>
     * {@code null} 表示该消息未触发违规；
     * 非 {@code null} 表示该消息被合规审查拦截。
     * </p>
     */
    private ComplianceResult reviewResult;

    // ======================== Convenience constructors ========================

    /**
     * 构造普通消息（无合规审查结果）。
     */
    public ChatMessage(String role, String content, LocalDateTime timestamp) {
        this(role, content, timestamp, null);
    }
}