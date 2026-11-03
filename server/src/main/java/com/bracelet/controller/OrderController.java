package com.bracelet.controller;

import com.bracelet.config.UserContext;
import com.bracelet.dto.OrderCreateDTO;
import com.bracelet.service.OrderService;
import com.bracelet.util.Result;
import com.bracelet.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * C 端订单 Controller —— 结算下单 / 我的订单 / mock 支付 / 取消 / 确认收货 / 退款申请
 */
@Slf4j
@RestController
@RequestMapping("/api/bracelet")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // ================================================================
    // 结算购物车勾选项 → 一单一方案批量生成
    // ================================================================
    @PostMapping("/orders")
    public Result<List<OrderVO>> create(@RequestBody OrderCreateDTO dto) {
        try {
            return Result.ok("下单成功", orderService.createOrders(UserContext.require(), dto));
        } catch (RuntimeException e) {
            log.warn("下单失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("下单异常", e);
            return Result.fail("服务器内部错误");
        }
    }

    // ================================================================
    // 我的订单列表（status 可选：0~6）
    // ================================================================
    @GetMapping("/orders")
    public Result<List<OrderVO>> list(@RequestParam(required = false) Integer status) {
        try {
            return Result.ok(orderService.listMyOrders(UserContext.require(), status));
        } catch (RuntimeException e) {
            log.warn("查询订单失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("查询订单异常", e);
            return Result.fail("服务器内部错误");
        }
    }

    // ================================================================
    // 订单详情
    // ================================================================
    @GetMapping("/orders/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        try {
            return Result.ok(orderService.getMyOrder(UserContext.require(), id));
        } catch (RuntimeException e) {
            log.warn("查询订单详情失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("查询订单详情异常", e);
            return Result.fail("服务器内部错误");
        }
    }

    // ================================================================
    // mock 支付：待付款 → 待发货
    // ================================================================
    @PostMapping("/orders/{id}/pay")
    public Result<String> pay(@PathVariable Long id) {
        try {
            orderService.pay(UserContext.require(), id);
            return Result.ok("支付成功", "已进入待发货");
        } catch (RuntimeException e) {
            log.warn("支付失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("支付异常", e);
            return Result.fail("服务器内部错误");
        }
    }

    // ================================================================
    // 取消订单：待付款 → 已取消
    // ================================================================
    @PostMapping("/orders/{id}/cancel")
    public Result<String> cancel(@PathVariable Long id) {
        try {
            orderService.cancel(UserContext.require(), id);
            return Result.ok("订单已取消", "已取消");
        } catch (RuntimeException e) {
            log.warn("取消订单失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("取消订单异常", e);
            return Result.fail("服务器内部错误");
        }
    }

    // ================================================================
    // 确认收货：待收货 → 已完成
    // ================================================================
    @PostMapping("/orders/{id}/confirm")
    public Result<String> confirm(@PathVariable Long id) {
        try {
            orderService.confirm(UserContext.require(), id);
            return Result.ok("已确认收货", "已完成");
        } catch (RuntimeException e) {
            log.warn("确认收货失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("确认收货异常", e);
            return Result.fail("服务器内部错误");
        }
    }

    // ================================================================
    // 申请退款：待发货/待收货 → 退款中
    // ================================================================
    @PostMapping("/orders/{id}/refund")
    public Result<String> refund(@PathVariable Long id) {
        try {
            orderService.requestRefund(UserContext.require(), id);
            return Result.ok("退款申请已提交", "退款中");
        } catch (RuntimeException e) {
            log.warn("申请退款失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("申请退款异常", e);
            return Result.fail("服务器内部错误");
        }
    }
}
