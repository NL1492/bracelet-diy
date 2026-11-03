package com.bracelet.dto;

import lombok.Data;

/**
 * 管理端重置密码 DTO
 */
@Data
public class AdminPasswordDTO {

    /** 新密码（6-32 位） */
    private String password;
}
