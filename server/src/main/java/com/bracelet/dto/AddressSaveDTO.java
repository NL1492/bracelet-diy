package com.bracelet.dto;

import lombok.Data;

/**
 * 收货地址新增 / 编辑入参
 */
@Data
public class AddressSaveDTO {

    /** 收货人 */
    private String receiverName;

    /** 收货电话 */
    private String receiverPhone;

    /** 省 / 市 / 区 */
    private String region;

    /** 详细地址（街道、门牌号等） */
    private String detail;

    /** 是否设为默认地址: 1是 0否（不传则保持原值） */
    private Integer isDefault;
}
