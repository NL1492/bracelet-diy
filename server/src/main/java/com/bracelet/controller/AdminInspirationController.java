package com.bracelet.controller;

import com.bracelet.dto.InspirationSaveDTO;
import com.bracelet.service.InspirationService;
import com.bracelet.util.Result;
import com.bracelet.vo.InspirationVO;
import com.bracelet.vo.ReviewVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理端灵感管理 Controller — 灵感 CRUD / 上下架 / 客订审核
 * <p>/** 由 AdminInterceptor 校验管理员身份
 */
@Slf4j
@RestController
@RequestMapping("/api/bracelet/admin")
@RequiredArgsConstructor
public class AdminInspirationController {

    private final InspirationService inspirationService;

    // ================================================================
    // 灵感列表（type 可选过滤：design / guest；含下架）
    // ================================================================
    @GetMapping("/inspirations")
    public Result<List<InspirationVO>> list(@RequestParam(required = false) String type) {
        try {
            return Result.ok(inspirationService.listAdmin(type));
        } catch (Exception e) {
            log.error("查询灵感列表异常", e);
            return Result.fail("服务器内部错误");
        }
    }

    // ================================================================
    // 新建灵感（设计新款）
    // ================================================================
    @PostMapping("/inspirations")
    public Result<Long> create(@RequestBody InspirationSaveDTO dto) {
        try {
            return Result.ok("灵感已创建", inspirationService.saveInspiration(dto));
        } catch (RuntimeException e) {
            log.warn("管理员新建灵感失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("管理员新建灵感异常", e);
            return Result.fail("服务器内部错误");
        }
    }

    // ================================================================
    // 编辑灵感（名称 / 价格 / 作者 / 珠子明细）
    // ================================================================
    @PutMapping("/inspirations/{id}")
    public Result<Long> update(@PathVariable Long id, @RequestBody InspirationSaveDTO dto) {
        try {
            dto.setId(id);
            return Result.ok("灵感已更新", inspirationService.saveInspiration(dto));
        } catch (RuntimeException e) {
            log.warn("管理员编辑灵感失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("管理员编辑灵感异常", e);
            return Result.fail("服务器内部错误");
        }
    }

    // ================================================================
    // 上架 / 下架
    // ================================================================
    @PutMapping("/inspirations/{id}/status")
    public Result<String> updateStatus(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        try {
            Boolean enable = body.get("enable");
            if (enable == null) {
                return Result.fail("缺少 enable 参数");
            }
            inspirationService.updateStatus(id, enable);
            return Result.ok(enable ? "已上架" : "已下架", null);
        } catch (RuntimeException e) {
            log.warn("灵感状态更新失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("灵感状态更新异常", e);
            return Result.fail("服务器内部错误");
        }
    }

    // ================================================================
    // 删除灵感
    // ================================================================
    @DeleteMapping("/inspirations/{id}")
    public Result<String> delete(@PathVariable Long id) {
        try {
            inspirationService.deleteInspiration(id);
            return Result.ok("已删除", null);
        } catch (RuntimeException e) {
            log.warn("管理员删除灵感失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("管理员删除灵感异常", e);
            return Result.fail("服务器内部错误");
        }
    }

    // ================================================================
    // 待审核客订列表（分享码采用次数达阈值）
    // ================================================================
    @GetMapping("/reviews")
    public Result<List<ReviewVO>> pendingReviews() {
        try {
            return Result.ok(inspirationService.pendingReviews());
        } catch (Exception e) {
            log.error("查询待审核列表异常", e);
            return Result.fail("服务器内部错误");
        }
    }

    // ================================================================
    // 审核通过 → 入库为优质客订灵感
    // ================================================================
    @PostMapping("/reviews/{designId}/approve")
    public Result<Long> approve(@PathVariable Long designId) {
        try {
            return Result.ok("已通过并入库灵感集录", inspirationService.approveReview(designId));
        } catch (RuntimeException e) {
            log.warn("客订审核通过失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("客订审核异常", e);
            return Result.fail("服务器内部错误");
        }
    }

    // ================================================================
    // 审核驳回
    // ================================================================
    @PostMapping("/reviews/{designId}/reject")
    public Result<String> reject(@PathVariable Long designId) {
        try {
            inspirationService.rejectReview(designId);
            return Result.ok("已驳回", null);
        } catch (RuntimeException e) {
            log.warn("客订审核驳回失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("客订审核驳回异常", e);
            return Result.fail("服务器内部错误");
        }
    }
}
