package com.scu.aicompliance.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户数据模型。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    /** 用户名 */
    private String username;

    /** BCrypt加密后的密码 */
    private String passwordHash;

    /** 角色（ADMIN/USER） */
    private String role;

    /** 账户状态：ACTIVE（正常）/ PENDING（待管理员审批） */
    private String status;

    public User(String username, String passwordHash, String role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = "ACTIVE";
    }
}