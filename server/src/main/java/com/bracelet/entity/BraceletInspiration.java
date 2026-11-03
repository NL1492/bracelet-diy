package com.bracelet.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 灵感集录实体 —— 对应 bracelet_inspiration 表
 * <p>type: design=设计新款（admin 设计器产出） / guest=优质客订（分享码审核入库）
 */
@Data
@TableName("bracelet_inspiration")
public class BraceletInspiration {

    /** 主键ID，自增长 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 灵感类型: design=设计新款 / guest=优质客订 */
    private String type;

    /** 灵感名称 */
    private String name;

    /** 展示售价 (元) */
    private BigDecimal price;

    /** 作者署名（设计新款=设计师名，客订=用户昵称） */
    private String author;

    /** 珠子明细 JSON: { "braceleted": true, "beads": [{"beadId":1,"size":10},...] } */
    private String beadsJson;

    /** 珠子总数量 */
    private Integer beadCount;

    /** 建议最小手围 (cm) */
    private BigDecimal wristMin;

    /** 建议最大手围 (cm) */
    private BigDecimal wristMax;

    /** 展示视频地址（预留，本期不做） */
    private String videoUrl;

    /** 来源方案ID（客订入库时关联 bracelet_design） */
    private Long designId;

    /** 被应用次数（C 端「应用此设计」+1） */
    private Integer useCount;

    /** 逻辑删除标记（MP 全局 logic-delete-field，仅删除时置 0；上下架语义见 status） */
    private Integer isEnable;

    /** 上架状态: 1=上架, 0=下架（独立于逻辑删除，admin 可自由切换） */
    private Integer status;

    /** 排序权重，越小越靠前 */
    private Integer sort;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
