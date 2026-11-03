-- 管理端操作审计日志表：记录管理员的所有写操作（POST/PUT/DELETE）
CREATE TABLE IF NOT EXISTS bracelet_admin_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id     BIGINT       NOT NULL COMMENT '操作人ID',
    username    VARCHAR(64)  DEFAULT NULL COMMENT '操作人用户名（冗余存储，便于追溯）',
    method      VARCHAR(10)  NOT NULL COMMENT 'HTTP 方法',
    uri         VARCHAR(200) NOT NULL COMMENT '请求路径',
    ip          VARCHAR(64)  DEFAULT NULL COMMENT '客户端 IP',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    KEY idx_admin_log_time (create_time),
    KEY idx_admin_log_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '管理员操作审计日志';
