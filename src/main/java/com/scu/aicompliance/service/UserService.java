package com.scu.aicompliance.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.scu.aicompliance.model.User;
import com.scu.aicompliance.util.JsonUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 用户服务类，负责用户数据的加载、查找和密码验证。
 * 使用 @Lazy 注入 BCryptPasswordEncoder 打破与 SecurityConfig 的循环依赖。
 */
@Service
public class UserService {

    private static final String USERS_FILE_PATH = "src/main/resources/users.json";

    private final BCryptPasswordEncoder passwordEncoder;
    private List<User> users = new ArrayList<>();

    public UserService(@Lazy BCryptPasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 初始化方法：检查users.json是否存在，不存在则创建默认用户。
     */
    @PostConstruct
    public void init() {
        File file = new File(USERS_FILE_PATH);
        if (file.exists()) {
            users = JsonUtil.readValue(file, new TypeReference<List<User>>() {});
        } else {
            createDefaultUsers();
            saveUsers();
        }
    }

    /**
     * 根据用户名查找用户。
     *
     * @param username 用户名
     * @return 包含用户的Optional，不存在则为空
     */
    public Optional<User> findByUsername(String username) {
        return users.stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst();
    }

    /**
     * 验证密码是否正确。
     *
     * @param username    用户名
     * @param rawPassword 原始密码
     * @return 验证成功返回true
     */
    public boolean validatePassword(String username, String rawPassword) {
        Optional<User> userOpt = findByUsername(username);
        if (userOpt.isEmpty()) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, userOpt.get().getPasswordHash());
    }

    /**
     * 将用户列表写入JSON文件。
     */
    private void saveUsers() {
        File file = new File(USERS_FILE_PATH);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        JsonUtil.writeValue(file, users);
    }

    /**
     * 创建默认管理员和普通用户。
     */
    private void createDefaultUsers() {
        users = new ArrayList<>();
        users.add(new User("admin", passwordEncoder.encode("admin123"), "ADMIN"));
        users.add(new User("user", passwordEncoder.encode("user123"), "USER"));
    }
}