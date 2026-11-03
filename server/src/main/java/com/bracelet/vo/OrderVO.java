package com.bracelet.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单 VO —— C 端「我的订单」与管理端订单列表共用
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderVO {

    /** 订单ID */
    private Long id;

    /** 业务单号 */
    private String orderNo;

    /** 关联方案ID */
    private Long designId;

    /** 方案名称 */
    private String designName;

    /** 珠子明细 JSON（前端渲染环形缩略图用） */
    private String beadListJson;

    /** 珠子总数量 */
    private Integer beadCount;

    /** 订单金额 */
    private BigDecimal totalAmount;

    /** 状态码: 0待付款 1待发货 2待收货 3已完成 4已取消 5退款中 6已退款 */
    private Integer status;

    /** 状态中文文案 */
    private String statusText;

    /** 收货人 */
    private String receiverName;

    /** 收货电话 */
    private String receiverPhone;

    /** 收货地址 */
    private String receiverAddress;

    /** 物流运单号 */
    private String shipNo;

    /** 备注 */
    private String remark;

    /** 申请退款前状态 */
    private Integer refundFromStatus;

    private LocalDateTime createTime;
    private LocalDateTime payTime;
    private LocalDateTime shipTime;
    private LocalDateTime finishTime;
    private LocalDateTime cancelTime;

    // ============ 管理端扩展字段 ============

    /** 下单用户昵称（管理端列表展示） */
    private String nickname;

    /** 下单用户名（管理端列表展示） */
    private String username;
}
