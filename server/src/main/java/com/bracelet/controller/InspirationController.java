package com.bracelet.controller;

import com.bracelet.config.UserContext;
import com.bracelet.service.InspirationService;
import com.bracelet.util.Result;
import com.bracelet.vo.InspirationVO;
import com.bracelet.vo.ShareVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 灵感集录 Controller — C 端公开列表 / 应用计数 / 分享码
 */
@Slf4j
@RestController
@RequestMapping("/api/bracelet")
@RequiredArgsConstructor
public class InspirationController {

    private final InspirationService inspirationService;

    // ================================================================
    // 接口1：灵感列表（公开，登录与否均可浏览）
    // 不带 type → { design: [...], guest: [...] } 分组
    // ================================================================
    @GetMapping("/inspirations")
    public Result<Map<String, List<InspirationVO>>> listPublic() {
        try {
            return Result.ok(inspirationService.listPublicGrouped());
        } catch (Exception e) {
            log.error("查询灵感列表异常", e);
            return Result.fail("服务器内部错误");
        }
    }

    // ================================================================
    // 接口2：灵感被应用计数 +1（公开，仅计数）
    // ================================================================
    @PostMapping("/inspirations/{id}/use")
    public Result<String> markUsed(@PathVariable Long id) {
        try {
            inspirationService.incrementUse(id);
            return Result.ok();
        } catch (Exception e) {
            log.error("灵感计数异常: id={}", id, e);
            return Result.fail("服务器内部错误");
        }
    }

    // ================================================================
    // 接口3：为我的设计方案生成分享码（需登录）
    // ================================================================
    @PostMapping("/designs/{id}/share")
    public Result<String> createShareCode(@PathVariable Long id) {
        try {
            return Result.ok("分享码已生成", inspirationService.createShareCode(UserContext.require(), id));
        } catch (RuntimeException e) {
            log.warn("生成分享码失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("生成分享码异常", e);
            return Result.fail("服务器内部错误");
        }
    }

    // ================================================================
    // 接口4：凭分享码载入方案（需登录），载入次数 +1
    // ================================================================
    @GetMapping("/share/{code}")
    public Result<ShareVO> loadByCode(@PathVariable String code) {
        try {
            return Result.ok(inspirationService.loadByCode(code));
        } catch (RuntimeException e) {
            log.warn("分享码载入失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("分享码载入异常", e);
            return Result.fail("服务器内部错误");
        }
    }
}
