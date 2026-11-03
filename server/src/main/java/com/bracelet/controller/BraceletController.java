package com.bracelet.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bracelet.config.UserContext;
import com.bracelet.dto.AddCartDTO;
import com.bracelet.dto.BeadItemDTO;
import com.bracelet.dto.SaveDesignDTO;
import com.bracelet.entity.BeadInfo;
import com.bracelet.service.BeadInfoService;
import com.bracelet.service.BraceletCartService;
import com.bracelet.service.BraceletDesignService;
import com.bracelet.util.Result;
import com.bracelet.vo.CartListVO;
import com.bracelet.vo.DesignListVO;
import com.bracelet.vo.OrderCountVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 手串DIY — 核心接口 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/bracelet")
@RequiredArgsConstructor
public class BraceletController {

    private final BraceletDesignService braceletDesignService;
    private final BraceletCartService braceletCartService;
    private final BeadInfoService beadInfoService;

    // ================================================================
    // 接口0：查询全部上架珠子信息（灵感详情构成解析使用）
    // ================================================================
    @GetMapping("/beads")
    public Result<List<BeadInfo>> listBeads() {
        try {
            List<BeadInfo> list = beadInfoService.list(
                    new LambdaQueryWrapper<BeadInfo>().orderByAsc(BeadInfo::getId));
            return Result.ok(list);
        } catch (Exception e) {
            log.error("查询珠子信息异常", e);
            return Result.fail("服务器内部错误");
        }
    }

    // ================================================================
    // 接口1：实时统计订单数据
    // ================================================================
    @PostMapping("/getCountData")
    public Result<OrderCountVO> getCountData(@RequestBody List<BeadItemDTO> beads) {
        try {
            OrderCountVO vo = braceletDesignService.getCountData(beads);
            return Result.ok(vo);
        } catch (RuntimeException e) {
            log.warn("统计失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("统计异常", e);
            return Result.fail("服务器内部错误");
        }
    }

    // ================================================================
    // 接口2：保存设计记录（新建 / 覆盖）
    // ================================================================
    @PostMapping("/saveDesign")
    public Result<Long> saveDesign(@RequestBody SaveDesignDTO dto) {
        try {
            if (dto.getDesignName() == null || dto.getDesignName().isBlank()) {
                return Result.fail("方案名称不能为空");
            }
            if (dto.getBeads() == null || dto.getBeads().isEmpty()) {
                return Result.fail("珠子明细不能为空");
            }
            Long designId = braceletDesignService.saveDesign(
                    UserContext.require(),
                    dto.getDesignId(),
                    dto.getDesignName().trim(),
                    dto.getBeads(),
                    dto.isBraceleted()
            );
            String action = dto.getDesignId() != null ? "覆盖" : "新建";
            log.info("{}方案成功: designId={}, name={}", action, designId, dto.getDesignName());
            return Result.ok(action + "成功", designId);
        } catch (RuntimeException e) {
            log.warn("保存设计失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("保存设计异常", e);
            return Result.fail("服务器内部错误");
        }
    }

    // ================================================================
    // 接口3：加入购物车
    // ================================================================
    @PostMapping("/addCart")
    public Result<Long> addCart(@RequestBody AddCartDTO dto) {
        try {
            if (dto.getBeads() == null || dto.getBeads().isEmpty()) {
                return Result.fail("珠子明细不能为空");
            }
            Long cartId = braceletCartService.addCart(
                    UserContext.require(), dto.getDesignId(), dto.getDesignName(), dto.getBeads());
            return Result.ok("已加入购物车", cartId);
        } catch (RuntimeException e) {
            log.warn("加购失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("加购异常", e);
            return Result.fail("服务器内部错误");
        }
    }

    // ================================================================
    // 接口4：查询设计方案列表
    // ================================================================
    @GetMapping("/designs")
    public Result<List<DesignListVO>> listDesigns() {
        try {
            List<DesignListVO> list = braceletDesignService.listDesigns(UserContext.require());
            return Result.ok(list);
        } catch (Exception e) {
            log.error("查询方案列表异常", e);
            return Result.fail("服务器内部错误");
        }
    }

    // ================================================================
    // 接口5：查询单个方案详情
    // ================================================================
    @GetMapping("/designs/{id}")
    public Result<DesignListVO> getDesign(@PathVariable Long id) {
        try {
            DesignListVO vo = braceletDesignService.getDesignDetail(UserContext.require(), id);
            if (vo == null) {
                return Result.fail("方案不存在");
            }
            return Result.ok(vo);
        } catch (Exception e) {
            log.error("查询方案详情异常", e);
            return Result.fail("服务器内部错误");
        }
    }

    // ================================================================
    // 接口6：查询购物车列表
    // ================================================================
    @GetMapping("/cart")
    public Result<List<CartListVO>> listCart() {
        try {
            List<CartListVO> list = braceletCartService.listCart(UserContext.require());
            return Result.ok(list);
        } catch (Exception e) {
            log.error("查询购物车列表异常", e);
            return Result.fail("服务器内部错误");
        }
    }

    // ================================================================
    // 接口7：删除购物车记录
    // ================================================================
    @DeleteMapping("/cart/{id}")
    public Result<String> deleteCart(@PathVariable Long id) {
        try {
            braceletCartService.deleteCart(UserContext.require(), id);
            return Result.ok("已移除");
        } catch (RuntimeException e) {
            log.warn("删除购物车失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("删除购物车异常", e);
            return Result.fail("服务器内部错误");
        }
    }

    // ================================================================
    // 接口8：删除设计方案（仅限本人）
    // ================================================================
    @DeleteMapping("/designs/{id}")
    public Result<String> deleteDesign(@PathVariable Long id) {
        try {
            braceletDesignService.deleteDesign(UserContext.require(), id);
            return Result.ok("已删除");
        } catch (RuntimeException e) {
            log.warn("删除方案失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("删除方案异常", e);
            return Result.fail("服务器内部错误");
        }
    }
}
