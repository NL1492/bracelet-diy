package com.bracelet.controller;

import com.bracelet.dto.OrderShipDTO;
import com.bracelet.service.OrderService;
import com.bracelet.util.Result;
import com.bracelet.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端订单 Controller —— 订单列表 / 详情 / 发货 / 退款审核
 * <p>由 AdminInterceptor 校验管理员身份
 */
@Slf4j
@RestController
@RequestMapping("/api/bracelet/admin")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    // ================================================================
    // 订单列表（status 可选过滤 + keyword 搜索单号/方案/用户）
    // ================================================================
    @GetMapping("/orders")
    public Result<List<OrderVO>> list(@RequestParam(required = false) Integer status,
                                      @RequestParam(required = false) String keyword) {
        try {
            return Result.ok(orderService.listAll(status, keyword));
        } catch (Exception e) {
            log.error("查询订单列表异常", e);
            return Result.fail("服务器内部错误");
        }
    }

    // ================================================================
    // 订单详情
    // ================================================================
    @GetMapping("/orders/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        try {
            return Result.ok(orderService.getDetail(id));
        } catch (RuntimeException e) {
            log.warn("查询订单详情失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("查询订单详情异常", e);
            return Result.fail("服务器内部错误");
        }
    }

    // ================================================================
    // 发货：待发货 → 待收货
    // ================================================================
    @PostMapping("/orders/{id}/ship")
    public Result<String> ship(@PathVariable Long id, @RequestBody OrderShipDTO dto) {
        try {
            orderService.ship(id, dto == null ? null : dto.getShipNo());
            return Result.ok("已发货", "待收货");
        } catch (RuntimeException e) {
            log.warn("发货失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("发货异常", e);
            return Result.fail("服务器内部错误");
        }
    }

    // ================================================================
    // 同意退款：退款中 → 已退款
    // ================================================================
    @PostMapping("/orders/{id}/refund/approve")
    public Result<String> approveRefund(@PathVariable Long id) {
        try {
            orderService.approveRefund(id);
            return Result.ok("退款已完成", "已退款");
        } catch (RuntimeException e) {
            log.warn("同意退款失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("同意退款异常", e);
            return Result.fail("服务器内部错误");
        }
    }

    // ================================================================
    // 驳回退款：退款中 → 回退原状态
    // ================================================================
    @PostMapping("/orders/{id}/refund/reject")
    public Result<String> rejectRefund(@PathVariable Long id) {
        try {
            orderService.rejectRefund(id);
            return Result.ok("已驳回退款", "订单已回退");
        } catch (RuntimeException e) {
            log.warn("驳回退款失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("驳回退款异常", e);
            return Result.fail("服务器内部错误");
        }
    }
}
