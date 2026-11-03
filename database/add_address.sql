-- ============================================================
-- 收货地址簿 —— 建表脚本
-- 数据库: bead
-- 执行方式: mysql -u root -p123123 -D bead --default-character-set=utf8mb4
--
-- 设计要点:
--   1. 一个用户可保存多个地址，is_default=1 为结算自动填充的默认地址
--   2. is_enable 只做逻辑删除（MyBatis-Plus 全局 logic-delete-field）
--   3. region 存省/市/区，detail 存街道门牌等详细地址，前端拼成完整地址
-- ============================================================

USE `bead`;

CREATE TABLE IF NOT EXISTS `bracelet_address` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id`        BIGINT       NOT NULL COMMENT '归属用户ID',
  `receiver_name`  VARCHAR(32)  NOT NULL COMMENT '收货人',
  `receiver_phone` VARCHAR(20)  NOT NULL COMMENT '收货电话',
  `region`         VARCHAR(128) NOT NULL COMMENT '省 / 市 / 区',
  `detail`         VARCHAR(255) NOT NULL COMMENT '详细地址（街道、门牌号等）',
  `is_default`     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '默认地址: 1是 0否',
  `is_enable`      TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '逻辑删除: 1正常 0删除',
  `create_time`    DATETIME     DEFAULT NULL COMMENT '创建时间',
  `update_time`    DATETIME     DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_default` (`user_id`, `is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址簿';

-- 验证
-- SELECT * FROM bracelet_address;
