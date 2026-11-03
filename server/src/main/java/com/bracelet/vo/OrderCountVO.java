package com.bracelet.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 订单统计返回 VO —— 前端订单信息卡渲染数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCountVO {

    /** 当前珠子总数 */
    private Integer beadCount;

    /** 珠子数量上限 */
    private Integer maxBeads;

    /** 预估总周长 (cm)，保留1位小数 */
    private BigDecimal totalCircumference;

    /** 建议手围最小值 (cm) */
    private BigDecimal wristMin;

    /** 建议手围最大值 (cm) */
    private BigDecimal wristMax;

    /** 手串总价 (元) */
    private BigDecimal totalPrice;
}
