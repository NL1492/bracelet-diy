package com.bracelet.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员操作审计日志实体 —— 对应 bracelet_admin_log 表
 */
@Data
@TableName("bracelet_admin_log")
public class BraceletAdminLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作人ID */
    private Long userId;

    /** 操作人用户名（冗余，便于追溯） */
    private String username;

    /** HTTP 方法（POST / PUT / DELETE） */
    private String method;

    /** 请求路径 */
    private String uri;

    /** 客户端 IP */
    private String ip;

    /** 操作时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
