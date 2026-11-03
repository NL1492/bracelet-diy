package com.bracelet.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 登录/注册/用户信息返回 VO
 */
@Data
@Builder
public class LoginVO {

    /** JWT 令牌（用户信息接口返回时为 null） */
    private String token;

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 头像地址 */
    private String avatar;

    /** 角色：user=普通用户，admin=管理员 */
    private String role;
}
