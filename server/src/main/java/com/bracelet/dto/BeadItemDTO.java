package com.bracelet.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 单颗珠子入参 DTO —— 前端传递画布上每颗珠子的标识与尺寸
 */
@Data
public class BeadItemDTO {

    /** 珠子ID，对应 bead_info 主键 */
    private Long beadId;

    /** 用户自定义缩放尺寸 (mm)，如 10 */
    private Integer size;
}
