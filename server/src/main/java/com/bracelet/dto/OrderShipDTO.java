package com.bracelet.dto;

import lombok.Data;

/**
 * 订单发货 DTO —— 管理员填写物流单号
 */
@Data
public class OrderShipDTO {

    /** 物流运单号 */
    private String shipNo;
}
