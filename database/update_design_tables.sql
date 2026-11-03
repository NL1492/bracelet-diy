-- ============================================================
-- 增量更新：创建设计方案表 + 购物车表
-- 适用：已导入 bead_info 但缺少 bracelet_design / bracelet_cart
-- 执行：mysql -u root -p < update_design_tables.sql
-- ============================================================

USE `bead`;

-- 设计方案表
DROP TABLE IF EXISTS `bracelet_design`;
CREATE TABLE `bracelet_design` (
  `id`                  INT UNSIGNED    NOT NULL AUTO_INCREMENT
    COMMENT '主键ID，自增长',

  `design_name`         VARCHAR(100)    NOT NULL
    COMMENT '方案名称，用户自定义',

  `bead_list_json`      TEXT            NOT NULL
    COMMENT '珠子明细JSON: {"braceleted":true,"beads":[{"beadId":1,"size":10},...]}',

  `bead_count`          INT             NOT NULL DEFAULT 0
    COMMENT '珠子总数量',

  `total_circumference` DECIMAL(5,1)    NOT NULL DEFAULT 0.0
    COMMENT '预估总周长 (cm)',

  `wrist_min`           DECIMAL(5,1)    NOT NULL DEFAULT 0.0
    COMMENT '建议最小手围 (cm)',

  `wrist_max`           DECIMAL(5,1)    NOT NULL DEFAULT 0.0
    COMMENT '建议最大手围 (cm)',

  `total_price`         DECIMAL(10,2)   NOT NULL DEFAULT 0.00
    COMMENT '总价 (元)',

  `create_time`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
    COMMENT '创建时间',

  `update_time`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    COMMENT '更新时间',

  PRIMARY KEY (`id`),
  KEY `idx_design_name` (`design_name`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='设计方案表';


-- 购物车表
DROP TABLE IF EXISTS `bracelet_cart`;
CREATE TABLE `bracelet_cart` (
  `id`                  INT UNSIGNED    NOT NULL AUTO_INCREMENT
    COMMENT '主键ID，自增长',

  `design_id`           INT UNSIGNED    DEFAULT NULL
    COMMENT '关联设计方案ID，直接加购时可为空',

  `design_name`         VARCHAR(100)    DEFAULT NULL
    COMMENT '方案名称，加购时由用户命名',

  `bead_list_json`      TEXT            NOT NULL
    COMMENT '珠子明细JSON',

  `bead_count`          INT             NOT NULL DEFAULT 0
    COMMENT '珠子总数量',

  `total_circumference` DECIMAL(5,1)    NOT NULL DEFAULT 0.0
    COMMENT '预估总周长 (cm)',

  `wrist_min`           DECIMAL(5,1)    NOT NULL DEFAULT 0.0
    COMMENT '建议最小手围 (cm)',

  `wrist_max`           DECIMAL(5,1)    NOT NULL DEFAULT 0.0
    COMMENT '建议最大手围 (cm)',

  `total_price`         DECIMAL(10,2)   NOT NULL DEFAULT 0.00
    COMMENT '总价 (元)',

  `create_time`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
    COMMENT '创建时间',

  PRIMARY KEY (`id`),
  KEY `idx_design_id` (`design_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='购物车表';

-- 验证
SELECT 'bracelet_design 已创建' AS result FROM DUAL;
SHOW CREATE TABLE `bracelet_design`\G

SELECT 'bracelet_cart 已创建' AS result FROM DUAL;
SHOW CREATE TABLE `bracelet_cart`\G
