package com.bracelet.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购物车实体 —— 对应 bracelet_cart 表
 */
@Data
@TableName("bracelet_cart")
public class BraceletCart {

    /** 主键ID，自增长 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 归属用户ID，0=历史数据（未归属） */
    private Long userId;

    /** 关联设计方案ID（可为空，直接加购时无方案） */
    private Long designId;

    /** 方案名称（加购时用户命名） */
    private String designName;

    /** 珠子明细 JSON: [{"beadId":1, "size":10}, ...] */
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
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
