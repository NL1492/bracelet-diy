package com.bracelet.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理端「经营总览」统计 VO —— 全部来自真实业务表聚合
 */
@Data
@Builder
public class OverviewStatsVO {

    /** 今日新增用户 */
    private long todayUsers;
    /** 昨日新增用户（算环比） */
    private long yesterdayUsers;
    /** 累计用户 */
    private long totalUsers;

    /** 今日成交订单数（已付款，不含退款/取消） */
    private long todayOrders;
    /** 昨日成交订单数 */
    private long yesterdayOrders;
    /** 今日成交额 */
    private BigDecimal todayGmv;
    /** 昨日成交额 */
    private BigDecimal yesterdayGmv;

    /** 累计成交订单数 */
    private long paidOrders;
    /** 累计成交额 */
    private BigDecimal paidGmv;

    /** 待发货 */
    private long pendingShip;
    /** 退款待审 */
    private long pendingRefund;
    /** 待付款 */
    private long pendingPay;
    /** 灵感客订待审核 */
    private long pendingReview;

    /** 近 7 日趋势 */
    private List<TrendPoint> trend;
    /** 近 30 日各类型珠子销量占比 */
    private List<HotType> hotTypes;
    /** 近 30 日销量 TOP5 珠子 */
    private List<TopBead> topBeads;

    @Data
    @Builder
    public static class TrendPoint {
        /** yyyy-MM-dd */
        private String date;
        /** 当日成交订单数 */
        private long orders;
        /** 当日成交额 */
        private BigDecimal gmv;
    }

    @Data
    @Builder
    public static class HotType {
        /** 类型名：主珠 / 隔片 / 配饰 / 其他 */
        private String label;
        /** 近 30 日售出颗数 */
        private long count;
        /** 占比（整数百分比） */
        private int pct;
    }

    @Data
    @Builder
    public static class TopBead {
        private String name;
        /** 售出颗数 */
        private long count;
        /** 单价 */
        private BigDecimal price;
        /** 素材图地址 */
        private String imgUrl;
        /** 占比（整数百分比） */
        private int pct;
    }
}
