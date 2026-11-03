package com.bracelet.dto;

import lombok.Data;

/**
 * 注册请求 DTO
 */
@Data
public class RegisterDTO {

    /** 用户名（3-20 位，中文/字母/数字/下划线） */
    private String username;

    /** 密码（6-32 位） */
    private String password;

    /** 昵称（可选，默认取用户名） */
    private String nickname;
}
