-- ============================================================
-- 增量更新：灵感集录表 + 设计方案表分享码/审核字段
-- 适用：已按 schema.sql + update_design_tables.sql 建库的环境
-- 执行：mysql -u root -p < add_inspiration.sql
-- ============================================================

USE `bead`;

-- 1. 灵感集录表（admin 灵感管理产出，C 端灵感页展示）
CREATE TABLE IF NOT EXISTS `bracelet_inspiration` (
  `id`          INT UNSIGNED   NOT NULL AUTO_INCREMENT
    COMMENT '主键ID，自增长',

  `type`        VARCHAR(16)    NOT NULL DEFAULT 'design'
    COMMENT '灵感类型: design=设计新款(官方) / guest=优质客订(审核入库)',

  `name`        VARCHAR(100)   NOT NULL
    COMMENT '灵感名称',

  `price`       DECIMAL(10,2)  NOT NULL DEFAULT 0.00
    COMMENT '展示售价 (元)，卡片角标用',

  `author`      VARCHAR(50)    DEFAULT NULL
    COMMENT '作者署名（设计新款=设计师名，客订=用户昵称）',

  `beads_json`  TEXT           NOT NULL
    COMMENT '珠子明细JSON: {"braceleted":true,"beads":[{"beadId":1,"size":10},...]}',

  `bead_count`  INT            NOT NULL DEFAULT 0
    COMMENT '珠子总数量',

  `wrist_min`   DECIMAL(5,1)   NOT NULL DEFAULT 0.0
    COMMENT '建议最小手围 (cm)',

  `wrist_max`   DECIMAL(5,1)   NOT NULL DEFAULT 0.0
    COMMENT '建议最大手围 (cm)',

  `video_url`   VARCHAR(255)   DEFAULT NULL
    COMMENT '展示视频地址（预留，本期不做）',

  `design_id`   INT UNSIGNED   DEFAULT NULL
    COMMENT '来源方案ID（客订入库时关联 bracelet_design，设计新款为空）',

  `use_count`   INT            NOT NULL DEFAULT 0
    COMMENT '被应用次数（C 端「应用此设计」+1）',

  `is_enable`   TINYINT        NOT NULL DEFAULT 1
    COMMENT '逻辑删除标记（MyBatis-Plus 全局 logic-delete-field，仅删除时置 0）',

  `status`      TINYINT        NOT NULL DEFAULT 1
    COMMENT '上架状态: 1=上架, 0=下架（独立于逻辑删除，admin 可自由切换）',

  `sort`        INT            NOT NULL DEFAULT 0
    COMMENT '排序权重，越小越靠前',

  `create_time` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP
    COMMENT '创建时间',

  `update_time` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    COMMENT '更新时间',

  PRIMARY KEY (`id`),
  KEY `idx_type_enable_sort` (`type`, `is_enable`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='灵感集录表';

-- 2. 设计方案表增加分享码 / 审核字段
ALTER TABLE `bracelet_design`
  ADD COLUMN `share_code`      VARCHAR(16) DEFAULT NULL COMMENT '分享码（唯一，NULL=未生成）',
  ADD COLUMN `share_use_count` INT         NOT NULL DEFAULT 0 COMMENT '分享码被载入次数',
  ADD COLUMN `reviewed`        TINYINT     NOT NULL DEFAULT 0 COMMENT '审核状态: 0=未审核 1=已入库灵感集录 2=已驳回',
  ADD UNIQUE KEY `uk_share_code` (`share_code`);

-- 验证
SELECT 'bracelet_inspiration 已创建' AS result FROM DUAL;
SHOW CREATE TABLE `bracelet_inspiration`\G
