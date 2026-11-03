package com.bracelet.dto;

import lombok.Data;

import java.util.List;

/**
 * 保存设计入参 DTO
 */
@Data
public class SaveDesignDTO {

    /** 方案ID — 传已有ID为覆盖，null 为新建 */
    private Long designId;

    /** 方案名称 */
    private String designName;

    /** 是否已收拢成手串 */
    private boolean braceleted;

    /** 珠子明细数组 */
    private List<BeadItemDTO> beads;
}
