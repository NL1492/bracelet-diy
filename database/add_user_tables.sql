-- ============================================================
-- 用户体系升级脚本 — bracelet_user 表 + 方案/购物车归属用户
-- 执行方式：在 bead 库上执行本脚本（幂等性有限，请勿重复执行）
-- ============================================================

USE `bead`;

-- ============================================================
-- 用户表
-- ============================================================
CREATE TABLE `bracelet_user` (
  `id`          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT
    COMMENT '主键ID，自增长',

  `username`    VARCHAR(50)      NOT NULL
    COMMENT '用户名，登录凭证，全局唯一',

  `password`    VARCHAR(100)     NOT NULL
    COMMENT '密码（BCrypt 加密存储，60 字符）',

  `nickname`    VARCHAR(50)      DEFAULT NULL
    COMMENT '昵称，前端展示用，默认取用户名',

  `avatar`      VARCHAR(500)     DEFAULT NULL
    COMMENT '头像图片地址（预留）',

  `role`        VARCHAR(10)      NOT NULL DEFAULT 'user'
    COMMENT '角色：user=普通用户，admin=管理员',

  `is_enable`   TINYINT UNSIGNED NOT NULL DEFAULT 1
    COMMENT '启用状态：1=正常，0=已注销（逻辑删除）',

  `create_time` DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP
    COMMENT '注册时间',

  `update_time` DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    COMMENT '更新时间',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='用户表 — 注册/登录凭证与基础资料';

-- ============================================================
-- 设计方案表：增加归属用户字段
-- ============================================================
ALTER TABLE `bracelet_design`
  ADD COLUMN `user_id` BIGINT UNSIGNED NOT NULL DEFAULT 0
    COMMENT '归属用户ID，0=历史数据（未归属）' AFTER `id`,
  ADD KEY `idx_user_id` (`user_id`);

-- ============================================================
-- 购物车表：增加归属用户字段
-- ============================================================
ALTER TABLE `bracelet_cart`
  ADD COLUMN `user_id` BIGINT UNSIGNED NOT NULL DEFAULT 0
    COMMENT '归属用户ID，0=历史数据（未归属）' AFTER `id`,
  ADD KEY `idx_user_id` (`user_id`);

-- ------------------------------------------------------------
-- （可选）将历史数据挂到指定用户：注册后拿到用户ID，替换 <用户ID> 执行
-- UPDATE `bracelet_design` SET `user_id` = <用户ID> WHERE `user_id` = 0;
-- UPDATE `bracelet_cart`   SET `user_id` = <用户ID> WHERE `user_id` = 0;
-- ------------------------------------------------------------
