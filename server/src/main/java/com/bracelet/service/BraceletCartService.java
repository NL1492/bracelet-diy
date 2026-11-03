package com.bracelet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bracelet.dto.BeadItemDTO;
import com.bracelet.entity.BraceletCart;
import com.bracelet.vo.CartListVO;

import java.util.List;

/**
 * 购物车 Service 接口
 */
public interface BraceletCartService extends IService<BraceletCart> {

    /**
     * 加入购物车
     *
     * @param userId     归属用户ID
     * @param designId   已保存方案ID（可选）
     * @param designName 方案名称（用户输入）
     * @param beads      珠子明细数组
     * @return 购物车记录ID
     */
    Long addCart(Long userId, Long designId, String designName, List<BeadItemDTO> beads);

    /**
     * 查询当前用户的购物车列表
     */
    List<CartListVO> listCart(Long userId);

    /**
     * 删除购物车记录（校验归属）
     */
    void deleteCart(Long userId, Long cartId);
}
