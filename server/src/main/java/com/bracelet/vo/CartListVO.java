package com.bracelet.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购物车列表项 VO —— 订单页面渲染
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartListVO {

    /** 购物车记录ID */
    private Long id;

    /** 关联方案ID（可为空） */
    private Long designId;

    /** 方案名称（从设计方案关联获取，直接加购则用默认名） */
    private String designName;

    /** 珠子明细 JSON */
    private String beadListJson;

    /** 珠子总数量 */
    private Integer beadCount;

    /** 预估总周长 (cm) */
    private BigDecimal totalCircumference;

    /** 建议最小手围 (cm) */
    private BigDecimal wristMin;

    /** 建议最大手围 (cm) */
    private BigDecimal wristMax;

    /** 总价 (元) */
    private BigDecimal totalPrice;

    /** 创建时间 */
    private LocalDateTime createTime;
}
