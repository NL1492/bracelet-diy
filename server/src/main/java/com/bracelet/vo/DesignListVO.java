package com.bracelet.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设计方案列表项 VO —— 历史面板渲染
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DesignListVO {

    /** 方案ID */
    private Long id;

    /** 方案名称 */
    private String designName;

    /** 珠子总数量 */
    private Integer beadCount;

    /** 总价 (元) */
    private BigDecimal totalPrice;

    /** 珠子明细 JSON（含 beadId，前端用于回显） */
    private String beadListJson;

    /** 创建时间 */
    private LocalDateTime createTime;
}
