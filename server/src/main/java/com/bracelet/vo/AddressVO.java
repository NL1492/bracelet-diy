package com.bracelet.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 收货地址返回 VO
 */
@Data
@Builder
public class AddressVO {

    /** 地址ID */
    private Long id;

    /** 收货人 */
    private String receiverName;

    /** 收货电话 */
    private String receiverPhone;

    /** 省 / 市 / 区 */
    private String region;

    /** 详细地址 */
    private String detail;

    /** 是否默认地址: 1是 0否 */
    private Integer isDefault;
}
