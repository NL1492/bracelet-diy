package com.bracelet.service;

import com.bracelet.dto.OrderCreateDTO;
import com.bracelet.vo.OrderVO;

import java.util.List;

/**
 * 订单 Service —— 一单一方案 + 状态机流转 + mock 支付
 */
public interface OrderService {

    // ==================== 状态常量 ====================
    int STATUS_UNPAID = 0;      // 待付款
    int STATUS_PAID = 1;        // 待发货
    int STATUS_SHIPPED = 2;     // 待收货
    int STATUS_FINISHED = 3;    // 已完成
    int STATUS_CANCELLED = 4;   // 已取消
    int STATUS_REFUNDING = 5;   // 退款中
    int STATUS_REFUNDED = 6;    // 已退款

    // ==================== C 端 ====================

    /** 结算购物车勾选项 → 逐项生成订单（一单一方案） */
    List<OrderVO> createOrders(Long userId, OrderCreateDTO dto);

    /** 我的订单列表（status 为空则全部） */
    List<OrderVO> listMyOrders(Long userId, Integer status);

    /** 我的订单详情（含归属校验） */
    OrderVO getMyOrder(Long userId, Long id);

    /** mock 支付：待付款 → 待发货 */
    void pay(Long userId, Long id);

    /** 取消订单：待付款 → 已取消 */
    void cancel(Long userId, Long id);

    /** 确认收货：待收货 → 已完成 */
    void confirm(Long userId, Long id);

    /** 申请退款：待发货/待收货 → 退款中 */
    void requestRefund(Long userId, Long id);

    // ==================== 管理端 ====================

    /** 全部订单（status 过滤 + 关键词搜索） */
    List<OrderVO> listAll(Integer status, String keyword);

    /** 订单详情 */
    OrderVO getDetail(Long id);

    /** 发货：待发货 → 待收货，记录运单号 */
    void ship(Long id, String shipNo);

    /** 同意退款：退款中 → 已退款 */
    void approveRefund(Long id);

    /** 驳回退款：退款中 → 回退到申请前状态 */
    void rejectRefund(Long id);
}
