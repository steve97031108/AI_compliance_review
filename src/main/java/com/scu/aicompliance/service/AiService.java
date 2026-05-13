package com.scu.aicompliance.service;

import com.scu.aicompliance.model.ChatMessage;

import java.util.List;

/**
 * AI 服务接口，定义与大语言模型交互的核心方法。
 */
public interface AiService {

    /**
     * 根据对话历史消息列表生成 AI 回复。
     *
     * @param messages 对话历史消息列表
     * @return AI 生成的回复内容
     */
    String generateResponse(List<ChatMessage> messages);

    /**
     * 单次提问接口，兼容简单场景。
     *
     * @param prompt 用户提问内容
     * @return AI 生成的回复内容
     */
    String generateResponse(String prompt);
}