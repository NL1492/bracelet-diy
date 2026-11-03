package com.bracelet.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 珠子信息实体 —— 对应 bead_info 表
 */
@Data
@TableName("bead_info")
public class BeadInfo {

    /** 主键ID，自增长唯一编号 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 珠子名称 */
    private String beadName;

    /** 珠子类型编码: 1=主珠, 2=隔片, 3=配饰 */
    private Integer beadType;

    /** 珠子原始直径 (cm) */
    private BigDecimal diameter;

    /** 单个珠子售价 (元) */
    private BigDecimal price;

    /** 珠子图片访问地址 */
    private String imgUrl;

    /** 排序数字 */
    private Integer sort;

    /** 启用状态: 1=上架, 0=下架 */
    private Integer isEnable;

    /** 备注内容 */
    private String remark;

    /** 数据创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 数据更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
