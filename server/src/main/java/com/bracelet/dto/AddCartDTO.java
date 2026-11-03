package com.bracelet.dto;

import lombok.Data;

import java.util.List;

/**
 * 加入购物车入参 DTO
 */
@Data
public class AddCartDTO {

    /** 已保存方案ID（可选，直接加购时可不传） */
    private Long designId;

    /** 方案名称（加购时用户命名） */
    private String designName;

    /** 珠子明细数组 */
    private List<BeadItemDTO> beads;
}
