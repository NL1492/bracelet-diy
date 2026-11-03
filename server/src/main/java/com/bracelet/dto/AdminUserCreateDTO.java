package com.bracelet.dto;

import lombok.Data;

/**
 * 管理端新增用户 DTO
 */
@Data
public class AdminUserCreateDTO {

    /** 用户名（3-20 位） */
    private String username;

    /** 密码（6-32 位） */
    private String password;

    /** 昵称（可选，默认取用户名） */
    private String nickname;

    /** 角色：user / admin（默认 user） */
    private String role;
}
