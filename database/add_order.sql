-- ============================================================
-- 定制手串订单模块 —— 建表脚本
-- 数据库: bead
-- 执行方式: mysql -u root -p < add_order.sql
--
-- 设计要点:
--   1. 一单一方案：购物车勾选 N 个方案结算 → 生成 N 个独立订单
--   2. design_snapshot 存下单时的方案完整快照，方案后续被改/删不影响订单展示
--   3. status 独立于 is_enable：status 走业务状态机，is_enable 只做逻辑删除
-- ============================================================

USE `bead`;

CREATE TABLE IF NOT EXISTS `bracelet_order` (
  `id`                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no`           VARCHAR(32)  NOT NULL COMMENT '业务单号 BR+yyyyMMddHHmmss+随机4位',
  `user_id`            BIGINT       NOT NULL COMMENT '归属用户ID',
  `design_id`          BIGINT       DEFAULT NULL COMMENT '关联方案ID（方案被删后此列仍保留原值）',
  `design_name`        VARCHAR(64)  DEFAULT NULL COMMENT '方案名称快照',
  `bead_list_json`     TEXT         COMMENT '珠子明细快照: [{"beadId":1,"size":10},...]',
  `design_snapshot`    TEXT         COMMENT '完整方案快照 JSON（含周长/手围/单价等）',
  `bead_count`         INT          DEFAULT 0 COMMENT '珠子总数量',
  `total_amount`       DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '订单金额（服务端写入，不采信前端）',
  `status`             TINYINT      NOT NULL DEFAULT 0 COMMENT '0待付款 1待发货 2待收货 3已完成 4已取消 5退款中 6已退款',
  `refund_from_status` TINYINT      DEFAULT NULL COMMENT '申请退款前的状态（1或2），驳回时回退用',
  `receiver_name`      VARCHAR(32)  DEFAULT NULL COMMENT '收货人',
  `receiver_phone`     VARCHAR(20)  DEFAULT NULL COMMENT '收货电话',
  `receiver_address`   VARCHAR(255) DEFAULT NULL COMMENT '收货地址',
  `ship_no`            VARCHAR(64)  DEFAULT NULL COMMENT '物流运单号',
  `remark`             VARCHAR(255) DEFAULT NULL COMMENT '订单备注',
  `pay_time`           DATETIME     DEFAULT NULL COMMENT '支付时间',
  `ship_time`          DATETIME     DEFAULT NULL COMMENT '发货时间',
  `finish_time`        DATETIME     DEFAULT NULL COMMENT '完成时间',
  `cancel_time`        DATETIME     DEFAULT NULL COMMENT '取消时间',
  `is_enable`          TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '逻辑删除: 1正常 0删除',
  `create_time`        DATETIME     DEFAULT NULL COMMENT '创建时间',
  `update_time`        DATETIME     DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_status` (`user_id`, `status`),
  KEY `idx_status_create` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定制手串订单表';

-- 验证
-- SELECT COUNT(*) FROM bracelet_order;
