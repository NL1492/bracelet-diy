-- ============================================================
-- 增量更新：购物车表添加 design_name 字段
-- 执行：mysql -u root -p < add_cart_design_name.sql
-- ============================================================

USE `bead`;

ALTER TABLE `bracelet_cart`
  ADD COLUMN `design_name` VARCHAR(100) DEFAULT NULL
    COMMENT '方案名称，加购时由用户命名'
    AFTER `design_id`;

-- 验证
SELECT 'bracelet_cart.design_name 已添加' AS result FROM DUAL;
SHOW COLUMNS FROM `bracelet_cart` LIKE 'design_name';
