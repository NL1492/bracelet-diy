package com.bracelet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bracelet.entity.BeadInfo;
import com.bracelet.entity.BraceletOrder;
import com.bracelet.entity.BraceletUser;
import com.bracelet.mapper.BeadInfoMapper;
import com.bracelet.mapper.BraceletOrderMapper;
import com.bracelet.mapper.BraceletUserMapper;
import com.bracelet.vo.OverviewStatsVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理端经营总览统计 —— 全部从 bracelet_user / bracelet_order / bead_info 实时聚合，不使用任何 mock
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OverviewService {

    private final BraceletOrderMapper orderMapper;
    private final BraceletUserMapper userMapper;
    private final BeadInfoMapper beadInfoMapper;
    private final InspirationService inspirationService;

    private final ObjectMapper json = new ObjectMapper();

    /** 计入成交的订单状态：1 待发货 / 2 待收货 / 3 已完成（5、6 退款中/已退款不计） */
    private static final Set<Integer> PAID = Set.of(1, 2, 3);

    /** bead_type 编码 → 展示名 */
    private static final Map<Integer, String> TYPE_LABEL = Map.of(1, "主珠", 2, "隔片", 3, "配饰");

    public OverviewStatsVO stats() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();
        LocalDateTime yStart = today.minusDays(1).atStartOfDay();
        LocalDateTime hotFrom = today.minusDays(30).atStartOfDay();

        long todayUsers = userMapper.selectCount(new LambdaQueryWrapper<BraceletUser>()
                .ge(BraceletUser::getCreateTime, todayStart)
                .lt(BraceletUser::getCreateTime, tomorrowStart));
        long yesterdayUsers = userMapper.selectCount(new LambdaQueryWrapper<BraceletUser>()
                .ge(BraceletUser::getCreateTime, yStart)
                .lt(BraceletUser::getCreateTime, todayStart));
        long totalUsers = userMapper.selectCount(null);

        // 订单量小，一次性取出在内存里分组（后续数据量大再改 SQL 聚合）
        List<BraceletOrder> orders = orderMapper.selectList(null);

        List<BraceletOrder> paidOrders = orders.stream()
                .filter(o -> o.getStatus() != null && PAID.contains(o.getStatus()))
                .collect(Collectors.toList());

        long todayOrders = paidOrders.stream()
                .filter(o -> inRange(o.getPayTime(), todayStart, tomorrowStart)).count();
        long yesterdayOrders = paidOrders.stream()
                .filter(o -> inRange(o.getPayTime(), yStart, todayStart)).count();
        BigDecimal todayGmv = sumGmv(paidOrders, todayStart, tomorrowStart);
        BigDecimal yesterdayGmv = sumGmv(paidOrders, yStart, todayStart);

        long paidTotal = paidOrders.size();
        BigDecimal gmvTotal = paidOrders.stream()
                .map(o -> o.getTotalAmount() == null ? BigDecimal.ZERO : o.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pendingShip = orders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 1).count();
        long pendingRefund = orders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 5).count();
        long pendingPay = orders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 0).count();
        long pendingReview = 0;
        try {
            List<?> reviews = inspirationService.pendingReviews();
            pendingReview = reviews == null ? 0 : reviews.size();
        } catch (Exception e) {
            log.warn("查询灵感待审核数量失败: {}", e.getMessage());
        }

        // 近 7 日趋势
        List<OverviewStatsVO.TrendPoint> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            LocalDateTime s = d.atStartOfDay(), e = d.plusDays(1).atStartOfDay();
            trend.add(OverviewStatsVO.TrendPoint.builder()
                    .date(d.toString())
                    .orders(paidOrders.stream().filter(o -> inRange(o.getPayTime(), s, e)).count())
                    .gmv(sumGmv(paidOrders, s, e))
                    .build());
        }

        // 近 30 日珠子销量：解析订单里的珠子快照（beadId）
        Map<Long, Long> beadCounts = new HashMap<>();
        for (BraceletOrder o : paidOrders) {
            if (o.getPayTime() == null || o.getPayTime().isBefore(hotFrom)) continue;
            parseBeadIds(o.getBeadListJson()).forEach(id -> beadCounts.merge(id, 1L, Long::sum));
        }

        long hotTotal = beadCounts.values().stream().mapToLong(Long::longValue).sum();
        Map<Integer, Integer> idToType = new HashMap<>();
        Map<Integer, String> idToName = new HashMap<>();
        Map<Integer, BigDecimal> idToPrice = new HashMap<>();
        Map<Integer, String> idToImg = new HashMap<>();
        if (!beadCounts.isEmpty()) {
            List<BeadInfo> infos = beadInfoMapper.selectBatchIds(beadCounts.keySet());
            for (BeadInfo b : infos) {
                idToType.put(b.getId().intValue(), b.getBeadType());
                idToName.put(b.getId().intValue(), b.getBeadName());
                idToPrice.put(b.getId().intValue(), b.getPrice());
                idToImg.put(b.getId().intValue(), b.getImgUrl());
            }
        }

        // 类型占比
        Map<String, Long> byType = new LinkedHashMap<>();
        TYPE_LABEL.values().forEach(l -> byType.put(l, 0L));
        byType.put("其他", 0L);
        beadCounts.forEach((id, c) -> {
            String label = TYPE_LABEL.getOrDefault(idToType.get(id.intValue()), "其他");
            byType.merge(label, c, Long::sum);
        });
        List<OverviewStatsVO.HotType> hotTypes = byType.entrySet().stream()
                .filter(en -> en.getValue() > 0)
                .map(en -> OverviewStatsVO.HotType.builder()
                        .label(en.getKey())
                        .count(en.getValue())
                        .pct(pct(en.getValue(), hotTotal))
                        .build())
                .collect(Collectors.toList());

        // TOP5 珠子
        List<OverviewStatsVO.TopBead> top = beadCounts.entrySet().stream()
                .filter(en -> idToName.containsKey(en.getKey().intValue()))
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(5)
                .map(en -> OverviewStatsVO.TopBead.builder()
                        .name(idToName.get(en.getKey().intValue()))
                        .count(en.getValue())
                        .price(idToPrice.get(en.getKey().intValue()))
                        .imgUrl(idToImg.get(en.getKey().intValue()))
                        .pct(pct(en.getValue(), hotTotal))
                        .build())
                .collect(Collectors.toList());

        return OverviewStatsVO.builder()
                .todayUsers(todayUsers).yesterdayUsers(yesterdayUsers).totalUsers(totalUsers)
                .todayOrders(todayOrders).yesterdayOrders(yesterdayOrders)
                .todayGmv(todayGmv).yesterdayGmv(yesterdayGmv)
                .paidOrders(paidTotal).paidGmv(gmvTotal)
                .pendingShip(pendingShip).pendingRefund(pendingRefund)
                .pendingPay(pendingPay).pendingReview(pendingReview)
                .trend(trend).hotTypes(hotTypes).topBeads(top)
                .build();
    }

    private boolean inRange(LocalDateTime t, LocalDateTime from, LocalDateTime to) {
        return t != null && !t.isBefore(from) && t.isBefore(to);
    }

    private BigDecimal sumGmv(List<BraceletOrder> list, LocalDateTime from, LocalDateTime to) {
        return list.stream()
                .filter(o -> inRange(o.getPayTime(), from, to))
                .map(o -> o.getTotalAmount() == null ? BigDecimal.ZERO : o.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int pct(long part, long total) {
        if (total <= 0) return 0;
        return (int) Math.round(part * 100.0 / total);
    }

    /** 从订单的 beadListJson 里取出所有 beadId（容错：字段缺失 / 非数组一律跳过） */
    private List<Long> parseBeadIds(String raw) {
        List<Long> ids = new ArrayList<>();
        if (raw == null || raw.isBlank()) return ids;
        try {
            JsonNode root = json.readTree(raw);
            JsonNode beads = root.isArray() ? root : root.path("beads");
            if (!beads.isArray()) return ids;
            for (JsonNode n : beads) {
                JsonNode idNode = n.path("beadId");
                if (idNode.isNumber() && idNode.asLong() > 0) {
                    ids.add(idNode.asLong());
                }
            }
        } catch (Exception e) {
            log.warn("解析订单珠子快照失败: {}", e.getMessage());
        }
        return ids;
    }
}
