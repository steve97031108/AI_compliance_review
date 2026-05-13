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
}