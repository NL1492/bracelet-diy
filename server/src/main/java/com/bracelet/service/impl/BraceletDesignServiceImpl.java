package com.bracelet.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bracelet.dto.BeadItemDTO;
import com.bracelet.entity.BeadInfo;
import com.bracelet.entity.BraceletDesign;
import com.bracelet.mapper.BraceletDesignMapper;
import com.bracelet.service.BeadInfoService;
import com.bracelet.service.BraceletDesignService;
import com.bracelet.vo.DesignListVO;
import com.bracelet.vo.OrderCountVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 设计方案 Service 实现 — 核心业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BraceletDesignServiceImpl
        extends ServiceImpl<BraceletDesignMapper, BraceletDesign>
        implements BraceletDesignService {

    private static final int MAX_BEADS = 25;
    private static final BigDecimal WRIST_DELTA_MIN = new BigDecimal("5.0");
    private static final BigDecimal WRIST_DELTA_MAX = new BigDecimal("4.3");

    private final BeadInfoService beadInfoService;

    // ==================== 公共统计方法 ====================

    @Override
    public OrderCountVO getCountData(List<BeadItemDTO> beads) {
        if (beads == null || beads.isEmpty()) {
            return OrderCountVO.builder()
                    .beadCount(0).maxBeads(MAX_BEADS)
                    .totalCircumference(BigDecimal.ZERO)
                    .wristMin(BigDecimal.ZERO).wristMax(BigDecimal.ZERO)
                    .totalPrice(BigDecimal.ZERO).build();
        }
        int count = beads.size();
        if (count > MAX_BEADS) {
            throw new RuntimeException("珠子数量超过上限 25 颗，当前 " + count + " 颗");
        }

        BigDecimal totalCircumference = BigDecimal.ZERO;
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (BeadItemDTO item : beads) {
            BeadInfo bead = beadInfoService.getById(item.getBeadId());
            if (bead == null) {
                throw new RuntimeException("珠子不存在: beadId=" + item.getBeadId());
            }
            BigDecimal beadWidth = BigDecimal.valueOf(item.getSize())
                    .divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP);
            totalCircumference = totalCircumference.add(beadWidth);
            if (bead.getPrice() != null) {
                totalPrice = totalPrice.add(bead.getPrice());
            }
        }

        BigDecimal wristMin = totalCircumference.subtract(WRIST_DELTA_MIN);
        BigDecimal wristMax = totalCircumference.subtract(WRIST_DELTA_MAX);
        if (wristMin.compareTo(BigDecimal.ZERO) < 0) wristMin = BigDecimal.ZERO;
        if (wristMax.compareTo(BigDecimal.ZERO) < 0) wristMax = BigDecimal.ZERO;

        totalCircumference = totalCircumference.setScale(1, RoundingMode.HALF_UP);
        wristMin = wristMin.setScale(1, RoundingMode.HALF_UP);
        wristMax = wristMax.setScale(1, RoundingMode.HALF_UP);
        totalPrice = totalPrice.setScale(0, RoundingMode.HALF_UP);

        return OrderCountVO.builder()
                .beadCount(count).maxBeads(MAX_BEADS)
                .totalCircumference(totalCircumference)
                .wristMin(wristMin).wristMax(wristMax)
                .totalPrice(totalPrice).build();
    }

    // ==================== 保存设计方案（新建/覆盖） ====================

    @Override
    public Long saveDesign(Long userId, Long designId, String designName,
                           List<BeadItemDTO> beads, boolean braceleted) {
        OrderCountVO countData = getCountData(beads);

        // 珠子数据包装 JSON：{ "braceleted": true, "beads": [...] }
        JSONObject beadJson = new JSONObject();
        beadJson.put("braceleted", braceleted);
        beadJson.put("beads", beads);

        BraceletDesign design;
        if (designId != null) {
            // 覆盖已有方案
            design = this.getById(designId);
            if (design == null) {
                throw new RuntimeException("方案不存在: id=" + designId);
            }
            if (design.getUserId() != null && design.getUserId() != 0
                    && !design.getUserId().equals(userId)) {
                throw new RuntimeException("无权修改该方案");
            }
            design.setDesignName(designName);
        } else {
            // 新建方案
            design = new BraceletDesign();
            design.setUserId(userId);
            design.setDesignName(designName);
        }

        design.setBeadListJson(beadJson.toJSONString());
        design.setBeadCount(countData.getBeadCount());
        design.setTotalCircumference(countData.getTotalCircumference());
        design.setWristMin(countData.getWristMin());
        design.setWristMax(countData.getWristMax());
        design.setTotalPrice(countData.getTotalPrice());

        boolean saved = this.saveOrUpdate(design);
        if (!saved) {
            throw new RuntimeException("保存设计方案失败");
        }
        log.info("设计方案已保存: id={}, name={}, beads={}, braceleted={}",
                design.getId(), designName, countData.getBeadCount(), braceleted);
        return design.getId();
    }

    // ==================== 方案列表 ====================

    @Override
    public List<DesignListVO> listDesigns(Long userId) {
        LambdaQueryWrapper<BraceletDesign> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BraceletDesign::getUserId, userId)
                .orderByDesc(BraceletDesign::getCreateTime);

        List<BraceletDesign> list = this.list(wrapper);
        return list.stream().map(d -> DesignListVO.builder()
                .id(d.getId())
                .designName(d.getDesignName())
                .beadCount(d.getBeadCount())
                .totalPrice(d.getTotalPrice())
                .beadListJson(d.getBeadListJson())
                .createTime(d.getCreateTime())
                .build()).collect(Collectors.toList());
    }

    @Override
    public DesignListVO getDesignDetail(Long userId, Long designId) {
        BraceletDesign d = this.getById(designId);
        if (d == null) return null;
        if (d.getUserId() != null && d.getUserId() != 0
                && !d.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问该方案");
        }
        return DesignListVO.builder()
                .id(d.getId())
                .designName(d.getDesignName())
                .beadCount(d.getBeadCount())
                .totalPrice(d.getTotalPrice())
                .beadListJson(d.getBeadListJson())
                .createTime(d.getCreateTime())
                .build();
    }

    @Override
    public void deleteDesign(Long userId, Long designId) {
        BraceletDesign d = this.getById(designId);
        if (d == null) {
            throw new RuntimeException("方案不存在或已删除");
        }
        if (d.getUserId() == null || !d.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除该方案");
        }
        // bracelet_design 无 is_enable 字段，removeById 走物理删除；订单/购物车已存珠子快照，不受影响
        this.removeById(designId);
        log.info("设计方案已删除: id={}, userId={}", designId, userId);
    }
}
