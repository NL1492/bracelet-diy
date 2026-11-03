package com.bracelet.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 手串设计方案实体 —— 对应 bracelet_design 表
 */
@Data
@TableName("bracelet_design")
public class BraceletDesign {

    /** 主键ID，自增长 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 归属用户ID，0=历史数据（未归属） */
    private Long userId;

    /** 方案名称 */
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

    /** 分享码（唯一，NULL=未生成） */
    private String shareCode;

    /** 分享码被载入次数 */
    private Integer shareUseCount;

    /** 审核状态: 0=未审核 1=已入库灵感集录 2=已驳回 */
    private Integer reviewed;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
