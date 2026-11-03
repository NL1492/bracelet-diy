package com.bracelet.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分享码载入 VO —— C 端凭码载入方案
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareVO {

    /** 设计方案ID */
    private Long designId;

    /** 方案名称 */
    private String designName;

    /** 方案作者（归属用户昵称） */
    private String ownerNickname;

    /** 珠子明细 JSON（含 beadId，前端回显） */
    private String beadListJson;

    /** 该分享码累计被载入次数 */
    private Integer shareUseCount;
}
