package com.bracelet.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bracelet.dto.InspirationSaveDTO;
import com.bracelet.entity.BraceletInspiration;
import com.bracelet.vo.InspirationVO;
import com.bracelet.vo.ReviewVO;
import com.bracelet.vo.ShareVO;

import java.util.List;
import java.util.Map;

/**
 * 灵感集录 Service — 管理端 CRUD / 分享码 / 客订审核
 */
public interface InspirationService extends IService<BraceletInspiration> {

    /** C 端灵感页：上架灵感按类型分组 { design: [...], guest: [...] } */
    Map<String, List<InspirationVO>> listPublicGrouped();

    /** admin 列表：全部灵感（type 为空返回全部），含下架 */
    List<InspirationVO> listAdmin(String type);

    /** admin 新建/编辑灵感（新建固定 type=design；编辑沿用原类型） */
    Long saveInspiration(InspirationSaveDTO dto);

    /** 上架/下架 */
    void updateStatus(Long id, boolean enable);

    /** 删除灵感（物理删除） */
    void deleteInspiration(Long id);

    /** C 端「应用此设计」：被应用次数 +1 */
    void incrementUse(Long id);

    /** 为设计方案生成分享码（已有则直接返回） */
    String createShareCode(Long userId, Long designId);

    /** 凭分享码载入方案（登录用户），载入次数 +1 */
    ShareVO loadByCode(String code);

    /** 待审核客订列表：分享码采用次数达阈值且未审核 */
    List<ReviewVO> pendingReviews();

    /** 审核通过：设计方案入库为优质客订灵感 */
    Long approveReview(Long designId);

    /** 审核驳回 */
    void rejectReview(Long designId);
}
