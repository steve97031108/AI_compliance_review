package com.scu.aicompliance.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.scu.aicompliance.model.ChatMessage;
import com.scu.aicompliance.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 对话持久化存储服务 — 按用户分离存储对话历史。
 * <p>
 * 每个用户的对话数据存储在 src/main/resources/conversations/{username}.json，
 * 支持加载、追加、清空和列出所有用户对话文件。
 * </p>
 */
@Service
public class ConversationStore {

    private static final Logger log = LoggerFactory.getLogger(ConversationStore.class);
    private static final Path CONVERSATIONS_DIR = Paths.get("src", "main", "resources", "conversations");

    /**
     * 确保对话存储目录存在。
     */
    private void ensureDir() {
        File dir = CONVERSATIONS_DIR.toFile();
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                log.info("已创建对话存储目录: {}", CONVERSATIONS_DIR);
            }
        }
    }

    /**
     * 获取指定用户的对话历史文件路径。
     */
    private Path getUserFile(String username) {
        return CONVERSATIONS_DIR.resolve(username + ".json");
    }

    /**
     * 加载指定用户的完整对话历史。
     *
     * @param username 用户名
     * @return 对话消息列表，若无历史则返回空列表
     */
    public List<ChatMessage> loadHistory(String username) {
        ensureDir();
        File file = getUserFile(username).toFile();
        if (!file.exists()) {
            return new ArrayList<>();
        }
        try {
            List<ChatMessage> history = JsonUtil.readValue(file,
                    new TypeReference<List<ChatMessage>>() {});
            return history != null ? history : new ArrayList<>();
        } catch (Exception e) {
            log.error("加载用户 {} 的对话历史失败", username, e);
            return new ArrayList<>();
        }
    }

    /**
     * 保存指定用户的完整对话历史（覆盖写入）。
     *
     * @param username 用户名
     * @param history  对话消息列表
     */
    public void saveHistory(String username, List<ChatMessage> history) {
        ensureDir();
        File file = getUserFile(username).toFile();
        JsonUtil.writeValue(file, history);
    }

    /**
     * 追加一条消息到指定用户的对话历史。
     *
     * @param username 用户名
     * @param message  消息对象
     */
    public void appendMessage(String username, ChatMessage message) {
        List<ChatMessage> history = loadHistory(username);
        history.add(message);
        saveHistory(username, history);
    }

    /**
     * 清空指定用户的对话历史。
     *
     * @param username 用户名
     */
    public void clearHistory(String username) {
        ensureDir();
        File file = getUserFile(username).toFile();
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                log.info("已清空用户 {} 的对话历史", username);
            }
        }
    }

    /**
     * 列出所有有对话记录的用户名。
     *
     * @return 用户名列表
     */
    public List<String> listAllUsers() {
        ensureDir();
        File dir = CONVERSATIONS_DIR.toFile();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) {
            return Collections.emptyList();
        }
        List<String> users = new ArrayList<>();
        for (File f : files) {
            String name = f.getName();
            users.add(name.substring(0, name.length() - 5)); // 去掉 .json 后缀
        }
        return users;
    }
}