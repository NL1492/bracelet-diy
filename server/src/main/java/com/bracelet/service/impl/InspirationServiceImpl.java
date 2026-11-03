package com.bracelet.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bracelet.dto.BeadItemDTO;
import com.bracelet.dto.InspirationSaveDTO;
import com.bracelet.entity.BraceletDesign;
import com.bracelet.entity.BraceletInspiration;
import com.bracelet.entity.BraceletUser;
import com.bracelet.mapper.BraceletDesignMapper;
import com.bracelet.mapper.BraceletInspirationMapper;
import com.bracelet.mapper.BraceletUserMapper;
import com.bracelet.service.BraceletDesignService;
import com.bracelet.service.InspirationService;
import com.bracelet.vo.InspirationVO;
import com.bracelet.vo.OrderCountVO;
import com.bracelet.vo.ReviewVO;
import com.bracelet.vo.ShareVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 灵感集录 Service 实现 — 管理端 CRUD / 分享码 / 客订审核
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InspirationServiceImpl
        extends ServiceImpl<BraceletInspirationMapper, BraceletInspiration>
        implements InspirationService {

    /** 分享码采用次数审核阈值：达到即进入待审核列表 */
    public static final int SHARE_REVIEW_THRESHOLD = 5;

    /** 分享码字符集：去除易混淆字符 0 O 1 I L */
    private static final String CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 16;

    private final BraceletDesignMapper braceletDesignMapper;
    private final BraceletUserMapper braceletUserMapper;
    private final BraceletDesignService braceletDesignService;

    private static final SecureRandom RANDOM = new SecureRandom();

    // ==================== 查询 ====================

    @Override
    public Map<String, List<InspirationVO>> listPublicGrouped() {
        List<InspirationVO> all = this.list(new LambdaQueryWrapper<BraceletInspiration>()
                        .eq(BraceletInspiration::getStatus, 1)
                        .orderByAsc(BraceletInspiration::getSort)
                        .orderByDesc(BraceletInspiration::getId))
                .stream().map(InspirationServiceImpl::toVO).collect(Collectors.toList());
        Map<String, List<InspirationVO>> grouped = new LinkedHashMap<>();
        grouped.put("design", all.stream().filter(v -> "design".equals(v.getType())).collect(Collectors.toList()));
        grouped.put("guest", all.stream().filter(v -> "guest".equals(v.getType())).collect(Collectors.toList()));
        return grouped;
    }

    @Override
    public List<InspirationVO> listAdmin(String type) {
        LambdaQueryWrapper<BraceletInspiration> wrapper = new LambdaQueryWrapper<>();
        if (type != null && !type.isBlank()) {
            wrapper.eq(BraceletInspiration::getType, type.trim());
        }
        wrapper.orderByAsc(BraceletInspiration::getSort)
                .orderByDesc(BraceletInspiration::getId);
        return this.list(wrapper).stream().map(InspirationServiceImpl::toVO).collect(Collectors.toList());
    }

    private static InspirationVO toVO(BraceletInspiration i) {
        return InspirationVO.builder()
                .id(i.getId()).type(i.getType()).name(i.getName())
                .price(i.getPrice()).author(i.getAuthor())
                .beadsJson(i.getBeadsJson()).beadCount(i.getBeadCount())
                .wristMin(i.getWristMin()).wristMax(i.getWristMax())
                .useCount(i.getUseCount()).status(i.getStatus())
                .videoUrl(i.getVideoUrl())
                .sort(i.getSort()).createTime(i.getCreateTime())
                .build();
    }

    // ==================== admin 保存 / 状态 / 删除 ====================

    @Override
    public Long saveInspiration(InspirationSaveDTO dto) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new RuntimeException("灵感名称不能为空");
        }
        if (dto.getBeads() == null || dto.getBeads().isEmpty()) {
            throw new RuntimeException("珠子明细不能为空");
        }

        // 复用设计统计：珠数 / 手围区间（价格用管理员填写值，未填则取成本统计）
        OrderCountVO stats = braceletDesignService.getCountData(dto.getBeads());

        JSONObject beadJson = new JSONObject();
        beadJson.put("braceleted", dto.getBraceleted() == null || dto.getBraceleted());
        beadJson.put("beads", dto.getBeads());

        BraceletInspiration ins;
        if (dto.getId() != null) {
            ins = this.getById(dto.getId());
            if (ins == null) {
                throw new RuntimeException("灵感不存在: id=" + dto.getId());
            }
        } else {
            ins = new BraceletInspiration();
            ins.setType("design");          // 新建只允许设计新款；客订仅经审核流写入
            ins.setIsEnable(1);
            ins.setStatus(1);
        }
        ins.setName(dto.getName().trim());
        ins.setAuthor(dto.getAuthor() == null || dto.getAuthor().isBlank()
                ? "官方设计师" : dto.getAuthor().trim());
        ins.setPrice(dto.getPrice() != null ? dto.getPrice() : stats.getTotalPrice());
        ins.setBeadsJson(beadJson.toJSONString());
        ins.setBeadCount(stats.getBeadCount());
        ins.setWristMin(stats.getWristMin());
        ins.setWristMax(stats.getWristMax());
        ins.setSort(dto.getSort() != null ? dto.getSort() : 0);

        ins.setVideoUrl(dto.getVideoUrl() != null && !dto.getVideoUrl().isBlank()
                ? dto.getVideoUrl().trim() : null);

        if (!this.saveOrUpdate(ins)) {
            throw new RuntimeException("保存灵感失败");
        }
        log.info("灵感已保存: id={}, name={}, type={}, beads={}",
                ins.getId(), ins.getName(), ins.getType(), stats.getBeadCount());
        return ins.getId();
    }

    @Override
    public void updateStatus(Long id, boolean enable) {
        if (this.getById(id) == null) {
            throw new RuntimeException("灵感不存在: id=" + id);
        }
        // 上下架走独立 status 字段（is_enable 是全局逻辑删除字段，只能由 removeById 修改）
        this.update(new LambdaUpdateWrapper<BraceletInspiration>()
                .eq(BraceletInspiration::getId, id)
                .set(BraceletInspiration::getStatus, enable ? 1 : 0));
        log.info("灵感状态更新: id={}, status={}", id, enable ? 1 : 0);
    }

    @Override
    public void deleteInspiration(Long id) {
        if (this.getById(id) == null) {
            throw new RuntimeException("灵感不存在: id=" + id);
        }
        this.removeById(id);
        log.info("灵感已删除: id={}", id);
    }

    @Override
    public void incrementUse(Long id) {
        this.update(new LambdaUpdateWrapper<BraceletInspiration>()
                .eq(BraceletInspiration::getId, id)
                .setSql("use_count = use_count + 1"));
    }

    // ==================== 分享码 ====================

    @Override
    public String createShareCode(Long userId, Long designId) {
        BraceletDesign design = braceletDesignMapper.selectById(designId);
        if (design == null) {
            throw new RuntimeException("方案不存在: id=" + designId);
        }
        if (design.getUserId() != null && design.getUserId() != 0
                && !design.getUserId().equals(userId)) {
            throw new RuntimeException("无权分享该方案");
        }
        if (design.getUserId() != null && design.getUserId() == 0) {
            throw new RuntimeException("历史数据缺少归属用户，无法生成分享码");
        }
        // 已有分享码直接复用（保证同一方案对外只有一个码）
        if (design.getShareCode() != null && !design.getShareCode().isBlank()) {
            return design.getShareCode();
        }
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = randomCode();
            if (braceletDesignMapper.selectCount(
                    new LambdaQueryWrapper<BraceletDesign>().eq(BraceletDesign::getShareCode, code)) > 0) {
                continue;
            }
            design.setShareCode(code);
            if (design.getShareUseCount() == null) design.setShareUseCount(0);
            braceletDesignMapper.updateById(design);
            log.info("分享码已生成: designId={}, code={}", designId, code);
            return code;
        }
        throw new RuntimeException("分享码生成失败，请稍后重试");
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return sb.toString();
    }

    @Override
    public ShareVO loadByCode(String code) {
        if (code == null || code.isBlank()) {
            throw new RuntimeException("分享码不能为空");
        }
        BraceletDesign design = braceletDesignMapper.selectOne(
                new LambdaQueryWrapper<BraceletDesign>().eq(BraceletDesign::getShareCode, code.trim()));
        if (design == null) {
            throw new RuntimeException("分享码无效");
        }
        // 载入次数 +1（采用次数达标后进入管理员审核）
        braceletDesignMapper.update(null, new LambdaUpdateWrapper<BraceletDesign>()
                .eq(BraceletDesign::getId, design.getId())
                .setSql("share_use_count = share_use_count + 1"));

        return ShareVO.builder()
                .designId(design.getId())
                .designName(design.getDesignName())
                .ownerNickname(nicknameOf(design.getUserId()))
                .beadListJson(design.getBeadListJson())
                .shareUseCount((design.getShareUseCount() == null ? 0 : design.getShareUseCount()) + 1)
                .build();
    }

    // ==================== 客订审核 ====================

    @Override
    public List<ReviewVO> pendingReviews() {
        List<BraceletDesign> list = braceletDesignMapper.selectList(
                new LambdaQueryWrapper<BraceletDesign>()
                        .ge(BraceletDesign::getShareUseCount, SHARE_REVIEW_THRESHOLD)
                        .eq(BraceletDesign::getReviewed, 0)
                        .orderByDesc(BraceletDesign::getShareUseCount));
        return list.stream().map(d -> ReviewVO.builder()
                .designId(d.getId())
                .designName(d.getDesignName())
                .ownerNickname(nicknameOf(d.getUserId()))
                .shareUseCount(d.getShareUseCount())
                .beadCount(d.getBeadCount())
                .totalPrice(d.getTotalPrice())
                .beadListJson(d.getBeadListJson())
                .createTime(d.getCreateTime())
                .build()).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long approveReview(Long designId) {
        BraceletDesign design = braceletDesignMapper.selectById(designId);
        if (design == null) {
            throw new RuntimeException("方案不存在: id=" + designId);
        }
        if (design.getReviewed() != null && design.getReviewed() != 0) {
            throw new RuntimeException("该方案已处理过审核");
        }
        BraceletInspiration ins = new BraceletInspiration();
        ins.setType("guest");
        ins.setName(design.getDesignName());
        ins.setPrice(design.getTotalPrice());
        ins.setAuthor(nicknameOf(design.getUserId()));
        ins.setBeadsJson(design.getBeadListJson());
        ins.setBeadCount(design.getBeadCount());
        ins.setWristMin(design.getWristMin());
        ins.setWristMax(design.getWristMax());
        ins.setDesignId(design.getId());
        ins.setUseCount(0);
        ins.setIsEnable(1);
        ins.setStatus(1);
        ins.setSort(0);
        this.save(ins);

        design.setReviewed(1);
        braceletDesignMapper.updateById(design);
        log.info("客订审核通过: designId={} → inspirationId={}", designId, ins.getId());
        return ins.getId();
    }

    @Override
    public void rejectReview(Long designId) {
        BraceletDesign design = braceletDesignMapper.selectById(designId);
        if (design == null) {
            throw new RuntimeException("方案不存在: id=" + designId);
        }
        design.setReviewed(2);
        braceletDesignMapper.updateById(design);
        log.info("客订审核驳回: designId={}", designId);
    }

    private String nicknameOf(Long userId) {
        if (userId == null || userId == 0) return "佚名用户";
        BraceletUser user = braceletUserMapper.selectById(userId);
        return user != null && user.getNickname() != null && !user.getNickname().isBlank()
                ? user.getNickname() : "佚名用户";
    }
}
