package com.bracelet.dto;

import lombok.Data;

/**
 * 登录请求 DTO
 */
@Data
public class LoginDTO {

    /** 用户名 */
    private String username;

    /** 密码（明文传输，HTTPS/内网环境下由后端 BCrypt 校验） */
    private String password;
}
