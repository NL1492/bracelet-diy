package com.bracelet.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体 —— 对应 bracelet_user 表
 */
@Data
@TableName("bracelet_user")
public class BraceletUser {

    /** 主键ID，自增长 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名，登录凭证，全局唯一 */
    private String username;

    /** 密码（BCrypt 加密存储） */
    private String password;

    /** 昵称，前端展示用，默认取用户名 */
    private String nickname;

    /** 头像图片地址（预留） */
    private String avatar;

    /** 角色：user=普通用户，admin=管理员 */
    private String role;

    /** 启用状态：1=正常，0=已注销（逻辑删除） */
    private Integer isEnable;

    /** 注册时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
