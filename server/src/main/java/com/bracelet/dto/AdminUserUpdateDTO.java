package com.bracelet.dto;

import lombok.Data;

/**
 * 管理端编辑用户 DTO（昵称 / 角色）
 */
@Data
public class AdminUserUpdateDTO {

    /** 昵称 */
    private String nickname;

    /** 角色：user / admin */
    private String role;
}
