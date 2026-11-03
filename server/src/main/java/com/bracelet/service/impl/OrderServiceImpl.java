package com.bracelet.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bracelet.dto.OrderCreateDTO;
import com.bracelet.entity.BraceletCart;
import com.bracelet.entity.BraceletOrder;
import com.bracelet.entity.BraceletUser;
import com.bracelet.mapper.BraceletCartMapper;
import com.bracelet.mapper.BraceletOrderMapper;
import com.bracelet.mapper.BraceletUserMapper;
import com.bracelet.service.BraceletCartService;
import com.bracelet.service.OrderService;
import com.bracelet.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 订单 Service 实现
 * <p>状态机: 0待付款 →(支付)1待发货 →(发货)2待收货 →(确认)3已完成
 * <p>　　　　0 →(取消)4已取消；1/2 →(申请退款)5退款中 →(同意)6已退款 / (驳回)回退原状态
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl
        extends ServiceImpl<BraceletOrderMapper, BraceletOrder>
        implements OrderService {

    private final BraceletCartService braceletCartService;
    private final BraceletCartMapper braceletCartMapper;
    private final BraceletUserMapper braceletUserMapper;

    private static final DateTimeFormatter NO_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** 状态迁移白名单：当前状态 → 允许的下一状态 */
    private static final Map<Integer, Set<Integer>> ALLOWED = Map.of(
            STATUS_UNPAID, Set.of(STATUS_PAID, STATUS_CANCELLED),
            STATUS_PAID, Set.of(STATUS_SHIPPED, STATUS_REFUNDING),
            STATUS_SHIPPED, Set.of(STATUS_FINISHED, STATUS_REFUNDING),
            STATUS_REFUNDING, Set.of(STATUS_REFUNDED, STATUS_PAID, STATUS_SHIPPED)
    );

    // ================================================================
    // C 端
    // ================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<OrderVO> createOrders(Long userId, OrderCreateDTO dto) {
        if (dto == null || dto.getCartIds() == null || dto.getCartIds().isEmpty()) {
            throw new RuntimeException("请先勾选要结算的方案");
        }
        if (isBlank(dto.getReceiverName())) {
            throw new RuntimeException("请填写收货人");
        }
        if (isBlank(dto.getReceiverPhone())) {
            throw new RuntimeException("请填写联系电话");
        }
        if (isBlank(dto.getReceiverAddress())) {
            throw new RuntimeException("请填写收货地址");
        }

        List<OrderVO> created = new ArrayList<>();
        for (Long cartId : dto.getCartIds()) {
            BraceletCart cart = braceletCartMapper.selectById(cartId);
            if (cart == null) {
                throw new RuntimeException("购物车记录不存在: id=" + cartId);
            }
            if (!userId.equals(cart.getUserId())) {
                throw new RuntimeException("无权结算他人的方案");
            }

            BraceletOrder order = new BraceletOrder();
            order.setOrderNo(genOrderNo());
            order.setUserId(userId);
            order.setDesignId(cart.getDesignId());
            order.setDesignName(isBlank(cart.getDesignName()) ? "未命名方案" : cart.getDesignName());
            order.setBeadListJson(cart.getBeadListJson());
            order.setBeadCount(cart.getBeadCount() == null ? 0 : cart.getBeadCount());
            // 金额取服务端写入购物车时的值，不采信前端
            order.setTotalAmount(cart.getTotalPrice() == null ? BigDecimal.ZERO : cart.getTotalPrice());
            order.setStatus(STATUS_UNPAID);
            order.setReceiverName(dto.getReceiverName().trim());
            order.setReceiverPhone(dto.getReceiverPhone().trim());
            order.setReceiverAddress(dto.getReceiverAddress().trim());
            order.setRemark(dto.getRemark());
            order.setIsEnable(1);
            order.setDesignSnapshot(buildSnapshot(cart));

            this.save(order);
            // 订单生成后移除对应购物车行
            braceletCartService.removeById(cartId);
            created.add(toVO(order));
        }

        log.info("创建订单成功: userId={}, count={}, orderNos={}", userId, created.size(),
                created.stream().map(OrderVO::getOrderNo).collect(Collectors.toList()));
        return created;
    }

    @Override
    public List<OrderVO> listMyOrders(Long userId, Integer status) {
        LambdaQueryWrapper<BraceletOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BraceletOrder::getUserId, userId);
        if (status != null) {
            wrapper.eq(BraceletOrder::getStatus, status);
        }
        wrapper.orderByDesc(BraceletOrder::getCreateTime);
        return this.list(wrapper).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public OrderVO getMyOrder(Long userId, Long id) {
        return toVO(requireOwnOrder(userId, id));
    }

    @Override
    public void pay(Long userId, Long id) {
        BraceletOrder order = requireOwnOrder(userId, id);
        transition(order, STATUS_PAID);
        order.setPayTime(LocalDateTime.now());
        this.updateById(order);
        log.info("mock 支付成功: orderNo={}, userId={}", order.getOrderNo(), userId);
    }

    @Override
    public void cancel(Long userId, Long id) {
        BraceletOrder order = requireOwnOrder(userId, id);
        transition(order, STATUS_CANCELLED);
        order.setCancelTime(LocalDateTime.now());
        this.updateById(order);
        log.info("订单已取消: orderNo={}, userId={}", order.getOrderNo(), userId);
    }

    @Override
    public void confirm(Long userId, Long id) {
        BraceletOrder order = requireOwnOrder(userId, id);
        transition(order, STATUS_FINISHED);
        order.setFinishTime(LocalDateTime.now());
        this.updateById(order);
        log.info("确认收货: orderNo={}, userId={}", order.getOrderNo(), userId);
    }

    @Override
    public void requestRefund(Long userId, Long id) {
        BraceletOrder order = requireOwnOrder(userId, id);
        Integer from = order.getStatus();
        transition(order, STATUS_REFUNDING);
        order.setRefundFromStatus(from);
        this.updateById(order);
        log.info("申请退款: orderNo={}, from={}", order.getOrderNo(), from);
    }

    // ================================================================
    // 管理端
    // ================================================================

    @Override
    public List<OrderVO> listAll(Integer status, String keyword) {
        LambdaQueryWrapper<BraceletOrder> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(BraceletOrder::getStatus, status);
        }
        wrapper.orderByDesc(BraceletOrder::getCreateTime);
        List<BraceletOrder> orders = this.list(wrapper);

        // 批量取用户信息，避免 N+1 查询
        Set<Long> userIds = orders.stream()
                .map(BraceletOrder::getUserId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, BraceletUser> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            braceletUserMapper.selectBatchIds(userIds)
                    .forEach(u -> userMap.put(u.getId(), u));
        }

        List<OrderVO> vos = orders.stream().map(o -> {
            OrderVO vo = toVO(o);
            BraceletUser u = userMap.get(o.getUserId());
            if (u != null) {
                vo.setNickname(u.getNickname());
                vo.setUsername(u.getUsername());
            }
            return vo;
        }).collect(Collectors.toList());

        if (isBlank(keyword)) {
            return vos;
        }
        String kw = keyword.trim().toLowerCase();
        return vos.stream().filter(vo ->
                contains(vo.getOrderNo(), kw)
                        || contains(vo.getDesignName(), kw)
                        || contains(vo.getUsername(), kw)
                        || contains(vo.getNickname(), kw)
                        || contains(vo.getReceiverName(), kw)
        ).collect(Collectors.toList());
    }

    @Override
    public OrderVO getDetail(Long id) {
        BraceletOrder order = this.getById(id);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        OrderVO vo = toVO(order);
        BraceletUser u = braceletUserMapper.selectById(order.getUserId());
        if (u != null) {
            vo.setNickname(u.getNickname());
            vo.setUsername(u.getUsername());
        }
        return vo;
    }

    @Override
    public void ship(Long id, String shipNo) {
        if (isBlank(shipNo)) {
            throw new RuntimeException("请填写物流单号");
        }
        BraceletOrder order = this.getById(id);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        transition(order, STATUS_SHIPPED);
        order.setShipNo(shipNo.trim());
        order.setShipTime(LocalDateTime.now());
        this.updateById(order);
        log.info("订单已发货: orderNo={}, shipNo={}", order.getOrderNo(), shipNo);
    }

    @Override
    public void approveRefund(Long id) {
        BraceletOrder order = this.getById(id);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        transition(order, STATUS_REFUNDED);
        this.updateById(order);
        log.info("退款已通过: orderNo={}", order.getOrderNo());
    }

    @Override
    public void rejectRefund(Long id) {
        BraceletOrder order = this.getById(id);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (order.getStatus() == null || order.getStatus() != STATUS_REFUNDING) {
            throw new RuntimeException("该订单当前不在退款审核中");
        }
        Integer back = order.getRefundFromStatus();
        if (back == null || !ALLOWED.getOrDefault(STATUS_REFUNDING, Set.of()).contains(back)) {
            back = STATUS_PAID;
        }
        order.setStatus(back);
        order.setRefundFromStatus(null);
        this.updateById(order);
        log.info("退款已驳回，订单回退: orderNo={}, backTo={}", order.getOrderNo(), back);
    }

    // ================================================================
    // 私有工具
    // ================================================================

    /** 校验订单归属（C 端操作） */
    private BraceletOrder requireOwnOrder(Long userId, Long id) {
        BraceletOrder order = this.getById(id);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!userId.equals(order.getUserId())) {
            throw new RuntimeException("无权操作该订单");
        }
        return order;
    }

    /** 状态机校验 + 迁移 */
    private void transition(BraceletOrder order, int to) {
        Integer from = order.getStatus();
        Set<Integer> allowed = from == null ? Set.of() : ALLOWED.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw new RuntimeException("当前状态不允许该操作（" + statusText(from) + " → " + statusText(to) + "）");
        }
        order.setStatus(to);
    }

    /** 生成唯一业务单号：BR + yyyyMMddHHmmss + 4位随机 */
    private String genOrderNo() {
        for (int i = 0; i < 5; i++) {
            String no = "BR" + LocalDateTime.now().format(NO_TS)
                    + ThreadLocalRandom.current().nextInt(1000, 10000);
            long exists = this.count(new LambdaQueryWrapper<BraceletOrder>()
                    .eq(BraceletOrder::getOrderNo, no));
            if (exists == 0) {
                return no;
            }
        }
        throw new RuntimeException("订单号生成失败，请重试");
    }

    /** 下单时刻的方案快照 */
    private String buildSnapshot(BraceletCart cart) {
        JSONObject snap = new JSONObject();
        snap.put("designName", cart.getDesignName());
        snap.put("beadListJson", cart.getBeadListJson());
        snap.put("beadCount", cart.getBeadCount());
        snap.put("totalCircumference", cart.getTotalCircumference());
        snap.put("wristMin", cart.getWristMin());
        snap.put("wristMax", cart.getWristMax());
        snap.put("totalPrice", cart.getTotalPrice());
        snap.put("snapshotTime", LocalDateTime.now().toString());
        return JSON.toJSONString(snap);
    }

    private OrderVO toVO(BraceletOrder o) {
        return OrderVO.builder()
                .id(o.getId())
                .orderNo(o.getOrderNo())
                .designId(o.getDesignId())
                .designName(o.getDesignName())
                .beadListJson(o.getBeadListJson())
                .beadCount(o.getBeadCount())
                .totalAmount(o.getTotalAmount())
                .status(o.getStatus())
                .statusText(statusText(o.getStatus()))
                .receiverName(o.getReceiverName())
                .receiverPhone(o.getReceiverPhone())
                .receiverAddress(o.getReceiverAddress())
                .shipNo(o.getShipNo())
                .remark(o.getRemark())
                .refundFromStatus(o.getRefundFromStatus())
                .createTime(o.getCreateTime())
                .payTime(o.getPayTime())
                .shipTime(o.getShipTime())
                .finishTime(o.getFinishTime())
                .cancelTime(o.getCancelTime())
                .build();
    }

    private String statusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case STATUS_UNPAID: return "待付款";
            case STATUS_PAID: return "待发货";
            case STATUS_SHIPPED: return "待收货";
            case STATUS_FINISHED: return "已完成";
            case STATUS_CANCELLED: return "已取消";
            case STATUS_REFUNDING: return "退款中";
            case STATUS_REFUNDED: return "已退款";
            default: return "未知";
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private boolean contains(String source, String kw) {
        return source != null && source.toLowerCase().contains(kw);
    }
}
