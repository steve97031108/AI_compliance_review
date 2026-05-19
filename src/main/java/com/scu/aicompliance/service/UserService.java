package com.scu.aicompliance.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.scu.aicompliance.model.User;
import com.scu.aicompliance.util.JsonUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 用户服务类，负责用户数据的加载、查找、密码验证、注册审批和个人信息修改。
 */
@Service
public class UserService {

    private static final String USERS_FILE_PATH = "src/main/resources/users.json";

    private final BCryptPasswordEncoder passwordEncoder;
    private List<User> users = new ArrayList<>();

    public UserService(BCryptPasswordEncoder passwordEncoder) {
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
            // 兼容旧数据：如果用户没有 status 字段，默认设为 ACTIVE
            for (User user : users) {
                if (user.getStatus() == null) {
                    user.setStatus("ACTIVE");
                }
            }
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
     * 查找所有状态为 ACTIVE 的用户（脱敏副本）。
     *
     * @return 用户列表（密码字段置空）
     */
    public List<User> findAllActiveUsers() {
        return users.stream()
                .filter(u -> "ACTIVE".equals(u.getStatus()))
                .map(this::desensitizedCopy)
                .collect(Collectors.toList());
    }

    /**
     * 查找所有状态为 PENDING 的用户（脱敏副本）。
     *
     * @return 待审批用户列表
     */
    public List<User> findPendingUsers() {
        return users.stream()
                .filter(u -> "PENDING".equals(u.getStatus()))
                .map(this::desensitizedCopy)
                .collect(Collectors.toList());
    }

    /**
     * 获取全部用户的脱敏副本（密码哈希置空，不影响内存数据）。
     *
     * @return 所有用户的副本列表（密码字段置空）
     */
    public List<User> getAllUsers() {
        return users.stream()
                .map(this::desensitizedCopy)
                .collect(Collectors.toList());
    }

    /**
     * 创建用户的脱敏副本（密码字段置空）。
     */
    private User desensitizedCopy(User source) {
        User copy = new User();
        copy.setUsername(source.getUsername());
        copy.setPasswordHash(null);
        copy.setRole(source.getRole());
        copy.setStatus(source.getStatus());
        return copy;
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

    // ======================== 用户注册与审批 ========================

    /**
     * 注册新用户（状态为 PENDING，需管理员审批）。
     *
     * @param username    用户名
     * @param rawPassword 明文密码
     * @return 注册结果消息
     */
    public String registerUser(String username, String rawPassword) {
        // 校验用户名
        if (username == null || username.trim().isEmpty()) {
            return "用户名不能为空";
        }
        username = username.trim();
        if (username.length() < 3 || username.length() > 20) {
            return "用户名长度需在 3-20 个字符之间";
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            return "用户名只能包含字母、数字和下划线";
        }

        // 校验密码
        if (rawPassword == null || rawPassword.length() < 6) {
            return "密码长度不能少于 6 位";
        }

        // 检查用户名是否已存在
        if (findByUsername(username).isPresent()) {
            return "用户名 '" + username + "' 已存在";
        }

        // 创建 PENDING 状态用户
        User newUser = new User(
                username,
                passwordEncoder.encode(rawPassword),
                "USER",
                "PENDING"
        );
        users.add(newUser);
        saveUsers();
        return null; // null 表示成功
    }

    /**
     * 管理员审批通过一个待审批用户。
     *
     * @param username 用户名
     * @return true 表示操作成功
     */
    public boolean approveUser(String username) {
        Optional<User> userOpt = findByUsername(username);
        if (userOpt.isPresent() && "PENDING".equals(userOpt.get().getStatus())) {
            userOpt.get().setStatus("ACTIVE");
            saveUsers();
            return true;
        }
        return false;
    }

    /**
     * 管理员拒绝一个待审批用户（直接删除）。
     *
     * @param username 用户名
     * @return true 表示操作成功
     */
    public boolean rejectUser(String username) {
        boolean removed = users.removeIf(u ->
                u.getUsername().equals(username) && "PENDING".equals(u.getStatus()));
        if (removed) {
            saveUsers();
        }
        return removed;
    }

    /**
     * 管理员删除任意用户（不允许删除自己）。
     *
     * @param adminUsername 操作的管理员用户名
     * @param targetUsername 目标用户名
     * @return 操作结果消息，null 表示成功
     */
    public String deleteUser(String adminUsername, String targetUsername) {
        if (adminUsername.equals(targetUsername)) {
            return "不能删除自己的账户";
        }
        Optional<User> target = findByUsername(targetUsername);
        if (target.isEmpty()) {
            return "用户 '" + targetUsername + "' 不存在";
        }
        if ("ADMIN".equals(target.get().getRole())) {
            return "不能删除管理员账户";
        }
        users.removeIf(u -> u.getUsername().equals(targetUsername));
        saveUsers();
        return null;
    }

    // ======================== 个人信息修改 ========================

    /**
     * 修改用户名。
     *
     * @param oldUsername 旧用户名
     * @param newUsername 新用户名
     * @return 操作结果消息，null 表示成功
     */
    public String updateUsername(String oldUsername, String newUsername) {
        if (newUsername == null || newUsername.trim().isEmpty()) {
            return "新用户名不能为空";
        }
        newUsername = newUsername.trim();
        if (newUsername.equals(oldUsername)) {
            return "新用户名与当前用户名相同";
        }
        if (newUsername.length() < 3 || newUsername.length() > 20) {
            return "用户名长度需在 3-20 个字符之间";
        }
        if (!newUsername.matches("^[a-zA-Z0-9_]+$")) {
            return "用户名只能包含字母、数字和下划线";
        }
        if (findByUsername(newUsername).isPresent()) {
            return "用户名 '" + newUsername + "' 已存在";
        }

        Optional<User> userOpt = findByUsername(oldUsername);
        if (userOpt.isEmpty()) {
            return "用户不存在";
        }

        userOpt.get().setUsername(newUsername);
        saveUsers();
        return null;
    }

    /**
     * 修改密码。
     *
     * @param username        用户名
     * @param oldPassword     旧密码（明文）
     * @param newPassword     新密码（明文）
     * @return 操作结果消息，null 表示成功
     */
    public String updatePassword(String username, String oldPassword, String newPassword) {
        Optional<User> userOpt = findByUsername(username);
        if (userOpt.isEmpty()) {
            return "用户不存在";
        }

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, userOpt.get().getPasswordHash())) {
            return "当前密码错误";
        }

        // 校验新密码
        if (newPassword == null || newPassword.length() < 6) {
            return "新密码长度不能少于 6 位";
        }
        if (oldPassword.equals(newPassword)) {
            return "新密码不能与当前密码相同";
        }

        userOpt.get().setPasswordHash(passwordEncoder.encode(newPassword));
        saveUsers();
        return null;
    }

    // ======================== 持久化 ========================

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
        users.add(new User("admin", passwordEncoder.encode("admin123"), "ADMIN", "ACTIVE"));
        users.add(new User("user", passwordEncoder.encode("user123"), "USER", "ACTIVE"));
    }
}