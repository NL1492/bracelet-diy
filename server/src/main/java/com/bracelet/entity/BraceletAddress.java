package com.bracelet.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收货地址实体 —— 对应 bracelet_address 表
 * <p>一个用户可保存多个地址，is_default=1 为结算时自动填充的默认地址
 */
@Data
@TableName("bracelet_address")
public class BraceletAddress {

    /** 主键ID，自增长 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 归属用户ID */
    private Long userId;

    /** 收货人 */
    private String receiverName;

    /** 收货电话 */
    private String receiverPhone;

    /** 省 / 市 / 区 */
    private String region;

    /** 详细地址（街道、门牌号等） */
    private String detail;

    /** 默认地址: 1是 0否 */
    private Integer isDefault;

    /** 逻辑删除标记（MP 全局 logic-delete-field） */
    private Integer isEnable;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
