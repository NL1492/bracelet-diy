package com.bracelet.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 灵感集录 VO —— C 端灵感页列表 / admin 管理列表共用
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspirationVO {

    /** 灵感ID */
    private Long id;

    /** 灵感类型: design / guest */
    private String type;

    /** 灵感名称 */
    private String name;

    /** 展示售价 (元) */
    private BigDecimal price;

    /** 作者署名 */
    private String author;

    /** 珠子明细 JSON（前端渲染缩略图 / 应用到工作台） */
    private String beadsJson;

    /** 珠子总数量 */
    private Integer beadCount;

    /** 建议最小手围 (cm) */
    private BigDecimal wristMin;

    /** 建议最大手围 (cm) */
    private BigDecimal wristMax;

    /** 被应用次数 */
    private Integer useCount;

    /** 上架状态: 1=上架 0=下架（仅 admin 列表使用） */
    private Integer status;

    /** 演示视频地址（可空） */
    private String videoUrl;

    /** 排序权重 */
    private Integer sort;

    /** 创建时间 */
    private LocalDateTime createTime;
}
