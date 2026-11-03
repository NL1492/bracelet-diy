package com.bracelet.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 定制手串订单实体 —— 对应 bracelet_order 表
 * <p>一单一方案：购物车勾选 N 个方案结算 → 生成 N 个独立订单
 * <p>design_snapshot 保存下单时刻的方案快照，方案后续变更不影响订单
 */
@Data
@TableName("bracelet_order")
public class BraceletOrder {

    /** 主键ID，自增长 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务单号 BR+yyyyMMddHHmmss+随机4位（唯一） */
    private String orderNo;

    /** 归属用户ID */
    private Long userId;

    /** 关联方案ID（方案被删后仍保留原值，仅作追溯） */
    private Long designId;

    /** 方案名称快照 */
    private String designName;

    /** 珠子明细快照: [{"beadId":1,"size":10},...] */
    private String beadListJson;

    /** 完整方案快照 JSON（周长/手围/单价等） */
    private String designSnapshot;

    /** 珠子总数量 */
    private Integer beadCount;

    /** 订单金额（服务端写入，不采信前端传值） */
    private BigDecimal totalAmount;

    /** 订单状态: 0待付款 1待发货 2待收货 3已完成 4已取消 5退款中 6已退款 */
    private Integer status;

    /** 申请退款前的状态（1或2），驳回退款时回退用 */
    private Integer refundFromStatus;

    /** 收货人 */
    private String receiverName;

    /** 收货电话 */
    private String receiverPhone;

    /** 收货地址 */
    private String receiverAddress;

    /** 物流运单号 */
    private String shipNo;

    /** 订单备注 */
    private String remark;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 发货时间 */
    private LocalDateTime shipTime;

    /** 完成时间 */
    private LocalDateTime finishTime;

    /** 取消时间 */
    private LocalDateTime cancelTime;

    /** 逻辑删除标记（MP 全局 logic-delete-field） */
    private Integer isEnable;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
