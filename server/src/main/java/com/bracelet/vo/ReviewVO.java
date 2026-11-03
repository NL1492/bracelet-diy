package com.bracelet.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客订审核列表项 VO —— 分享码采用次数达阈值的设计方案
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewVO {

    /** 设计方案ID */
    private Long designId;

    /** 方案名称 */
    private String designName;

    /** 方案归属用户昵称 */
    private String ownerNickname;

    /** 分享码被载入次数 */
    private Integer shareUseCount;

    /** 珠子总数量 */
    private Integer beadCount;

    /** 总价 (元) */
    private BigDecimal totalPrice;

    /** 珠子明细 JSON（admin 预览渲染） */
    private String beadListJson;

    /** 创建时间 */
    private LocalDateTime createTime;
}
