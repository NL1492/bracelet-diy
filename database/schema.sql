-- ============================================================
-- 珠子定制系统 - 数据库初始化脚本
-- 数据库: bead
-- 引擎: MySQL 5.7+ / 8.0+
-- 字符集: utf8mb4
-- ============================================================

CREATE DATABASE IF NOT EXISTS `bead`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `bead`;

-- ============================================================
-- 珠子信息主表
-- ============================================================
DROP TABLE IF EXISTS `bead_info`;
CREATE TABLE `bead_info` (
  `id`          INT UNSIGNED    NOT NULL AUTO_INCREMENT
    COMMENT '主键ID，珠子唯一编号，自增长，前端传递标识、后端快速查询',

  `bead_name`   VARCHAR(100)    NOT NULL
    COMMENT '珠子名称，前端素材分类栏展示该珠子名字，方便识别款式',

  `bead_type`   TINYINT UNSIGNED NOT NULL
    COMMENT '珠子类型编码：1=主珠，2=隔片，3=配饰，区分品类，实现前端素材分组展示',

  `diameter`    DECIMAL(4,2)    NOT NULL
    COMMENT '珠子原始直径，单位厘米(如1.00=10mm)，后端计算手串整体预估周长的核心数据',

  `price`       DECIMAL(10,2)   NOT NULL DEFAULT 0.00
    COMMENT '单个珠子售价(元)，后端累加所有选中珠子价格，算出订单总金额',

  `img_url`     VARCHAR(500)    DEFAULT NULL
    COMMENT '珠子图片访问地址，相对路径如 /水晶/白色系/和田白玉.png，前端页面加载渲染珠子预览效果图使用',

  `sort`        INT             NOT NULL DEFAULT 0
    COMMENT '排序数字，控制珠子在前端素材列表里上下展示先后顺序，数字越小越靠前',

  `is_enable`   TINYINT UNSIGNED NOT NULL DEFAULT 1
    COMMENT '启用状态：1=上架可选用，0=下架隐藏，管控款式是否对外可选',

  `remark`      VARCHAR(500)    DEFAULT NULL
    COMMENT '备注内容，可填写珠子材质、品类介绍、规格补充说明',

  `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
    COMMENT '数据创建时间，记录该珠子录入数据库的时间',

  `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    COMMENT '数据更新时间，修改珠子信息时自动同步刷新时间',

  PRIMARY KEY (`id`),
  KEY `idx_bead_type` (`bead_type`),
  KEY `idx_is_enable` (`is_enable`),
  KEY `idx_sort` (`sort`),
  KEY `idx_bead_name` (`bead_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='珠子信息主表 — 存储所有可选用珠子的完整属性';


-- ============================================================
-- 设计方案表 — 用户保存的手串搭配方案
-- ============================================================
DROP TABLE IF EXISTS `bracelet_design`;
CREATE TABLE `bracelet_design` (
  `id`                  INT UNSIGNED    NOT NULL AUTO_INCREMENT
    COMMENT '主键ID，自增长',

  `design_name`         VARCHAR(100)    NOT NULL
    COMMENT '方案名称，用户自定义',

  `bead_list_json`      TEXT            NOT NULL
    COMMENT '珠子明细JSON: [{"beadId":1, "size":10}, ...]',

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
  COMMENT='设计方案表 — 用户保存的手串搭配方案';


-- ============================================================
-- 购物车表 — 用户待下单的手串
-- ============================================================
DROP TABLE IF EXISTS `bracelet_cart`;
CREATE TABLE `bracelet_cart` (
  `id`                  INT UNSIGNED    NOT NULL AUTO_INCREMENT
    COMMENT '主键ID，自增长',

  `design_id`           INT UNSIGNED    DEFAULT NULL
    COMMENT '关联设计方案ID，直接加购时可为空',

  `design_name`         VARCHAR(100)    DEFAULT NULL
    COMMENT '方案名称，加购时由用户命名',

  `bead_list_json`      TEXT            NOT NULL
    COMMENT '珠子明细JSON: [{"beadId":1, "size":10}, ...]',

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
  COMMENT='购物车表 — 用户待下单的手串';
