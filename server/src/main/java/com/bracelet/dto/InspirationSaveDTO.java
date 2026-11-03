package com.bracelet.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 灵感保存 DTO（admin 设计器新建 / 编辑）
 */
@Data
public class InspirationSaveDTO {

    /** 灵感ID（编辑时必传，新建为空） */
    private Long id;

    /** 灵感类型: design=设计新款 / guest=优质客订（编辑客订时回传，新建仅 design） */
    private String type;

    /** 灵感名称 */
    private String name;

    /** 展示售价（空则取珠子成本统计价） */
    private BigDecimal price;

    /** 作者署名 */
    private String author;

    /** 是否成串（手串态 / 散珠态） */
    private Boolean braceleted;

    /** 珠子明细（逐颗）: [{"beadId":1,"size":10}, ...] */
    private List<BeadItemDTO> beads;

    /** 演示视频地址（C 端灵感页实况展示用，可空） */
    private String videoUrl;

    /** 排序权重（可选，默认 0） */
    private Integer sort;
}
