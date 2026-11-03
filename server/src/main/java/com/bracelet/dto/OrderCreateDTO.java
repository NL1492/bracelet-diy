package com.bracelet.dto;

import lombok.Data;

import java.util.List;

/**
 * 创建订单 DTO —— 购物车勾选结算
 * <p>cartIds 中每一项各生成一个独立订单（一单一方案）
 */
@Data
public class OrderCreateDTO {

    /** 待结算的购物车记录ID列表 */
    private List<Long> cartIds;

    /** 收货人 */
    private String receiverName;

    /** 收货电话 */
    private String receiverPhone;

    /** 收货地址 */
    private String receiverAddress;

    /** 订单备注 */
    private String remark;
}
