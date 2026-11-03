-- ============================================================
-- 用户角色升级脚本 — bracelet_user 增加 role 字段
-- 适用环境：已执行过 add_user_tables.sql 的数据库
-- ============================================================

USE `bead`;

ALTER TABLE `bracelet_user`
  ADD COLUMN `role` VARCHAR(10) NOT NULL DEFAULT 'user'
    COMMENT '角色：user=普通用户，admin=管理员' AFTER `avatar`;

-- 管理员账号无需手动 INSERT：后端启动时会自动初始化
-- 默认账号 admin / admin123（BCrypt 加密存储，见 AdminInitializer）
