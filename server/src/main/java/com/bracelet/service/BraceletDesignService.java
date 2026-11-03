package com.bracelet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bracelet.dto.BeadItemDTO;
import com.bracelet.entity.BraceletDesign;
import com.bracelet.vo.DesignListVO;
import com.bracelet.vo.OrderCountVO;

import java.util.List;

/**
 * 设计方案 Service 接口
 */
public interface BraceletDesignService extends IService<BraceletDesign> {

    /**
     * 实时统计订单数据
     */
    OrderCountVO getCountData(List<BeadItemDTO> beads);

    /**
     * 保存设计记录（新建或覆盖）
     *
     * @param userId     归属用户ID
     * @param designId   方案ID，传 null 为新建，传已有ID 为覆盖
     * @param designName 方案名称
     * @param beads      珠子明细数组
     * @param braceleted 是否已收拢成手串
     * @return 方案ID
     */
    Long saveDesign(Long userId, Long designId, String designName, List<BeadItemDTO> beads, boolean braceleted);

    /**
     * 查询当前用户的设计方案列表（按创建时间倒序）
     */
    List<DesignListVO> listDesigns(Long userId);

    /**
     * 查询单个方案完整数据（校验归属）
     */
    DesignListVO getDesignDetail(Long userId, Long designId);

    /**
     * 删除方案（仅限方案归属人）
     */
    void deleteDesign(Long userId, Long designId);
}
