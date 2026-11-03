package com.bracelet.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bracelet.dto.BeadItemDTO;
import com.bracelet.entity.BraceletCart;
import com.bracelet.entity.BraceletDesign;
import com.bracelet.mapper.BraceletCartMapper;
import com.bracelet.mapper.BraceletDesignMapper;
import com.bracelet.service.BraceletCartService;
import com.bracelet.service.BraceletDesignService;
import com.bracelet.vo.CartListVO;
import com.bracelet.vo.OrderCountVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 购物车 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BraceletCartServiceImpl
        extends ServiceImpl<BraceletCartMapper, BraceletCart>
        implements BraceletCartService {

    private final BraceletDesignService braceletDesignService;
    private final BraceletDesignMapper braceletDesignMapper;

    @Override
    public Long addCart(Long userId, Long designId, String designName, List<BeadItemDTO> beads) {
        // 1. 调用统计方法算出整套数据
        OrderCountVO countData = braceletDesignService.getCountData(beads);

        // 2. 构建购物车记录
        BraceletCart cart = new BraceletCart();
        cart.setUserId(userId);
        cart.setDesignId(designId);
        cart.setDesignName(designName != null && !designName.isBlank() ? designName.trim() : "未命名方案");
        cart.setBeadListJson(JSON.toJSONString(beads));
        cart.setBeadCount(countData.getBeadCount());
        cart.setTotalCircumference(countData.getTotalCircumference());
        cart.setWristMin(countData.getWristMin());
        cart.setWristMax(countData.getWristMax());
        cart.setTotalPrice(countData.getTotalPrice());

        boolean saved = this.save(cart);
        if (!saved) {
            throw new RuntimeException("加入购物车失败");
        }

        log.info("已加入购物车: cartId={}, name={}, beads={}, price={}",
                cart.getId(), cart.getDesignName(), countData.getBeadCount(), countData.getTotalPrice());

        return cart.getId();
    }

    @Override
    public List<CartListVO> listCart(Long userId) {
        LambdaQueryWrapper<BraceletCart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BraceletCart::getUserId, userId)
                .orderByDesc(BraceletCart::getCreateTime);

        List<BraceletCart> list = this.list(wrapper);
        return list.stream().map(cart -> {
            String designName = cart.getDesignName();
            if (designName == null || designName.isBlank()) {
                // 兼容旧数据：尝试从关联方案获取名称
                if (cart.getDesignId() != null) {
                    BraceletDesign design = braceletDesignMapper.selectById(cart.getDesignId());
                    if (design != null && design.getDesignName() != null) {
                        designName = design.getDesignName();
                    }
                }
                if (designName == null || designName.isBlank()) {
                    designName = "未命名方案";
                }
            }
            return CartListVO.builder()
                    .id(cart.getId())
                    .designId(cart.getDesignId())
                    .designName(designName)
                    .beadListJson(cart.getBeadListJson())
                    .beadCount(cart.getBeadCount())
                    .totalCircumference(cart.getTotalCircumference())
                    .wristMin(cart.getWristMin())
                    .wristMax(cart.getWristMax())
                    .totalPrice(cart.getTotalPrice())
                    .createTime(cart.getCreateTime())
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public void deleteCart(Long userId, Long cartId) {
        BraceletCart cart = this.getById(cartId);
        if (cart == null) {
            throw new RuntimeException("购物车记录不存在: id=" + cartId);
        }
        if (cart.getUserId() != null && cart.getUserId() != 0
                && !cart.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作该购物车记录");
        }
        boolean removed = this.removeById(cartId);
        if (!removed) {
            throw new RuntimeException("购物车记录不存在: id=" + cartId);
        }
        log.info("已移除购物车记录: cartId={}, userId={}", cartId, userId);
    }
}
