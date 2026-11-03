package com.bracelet.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端用户列表 VO（不含密码）
 */
@Data
@Builder
public class AdminUserVO {

    /** 用户ID */
    private Long id;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 角色：user=普通用户，admin=管理员 */
    private String role;

    /** 启用状态：1=正常，0=已注销 */
    private Integer isEnable;

    /** 注册时间 */
    private LocalDateTime createTime;
}
