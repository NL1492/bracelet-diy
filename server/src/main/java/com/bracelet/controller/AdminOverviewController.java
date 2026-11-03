package com.bracelet.controller;

import com.bracelet.service.OverviewService;
import com.bracelet.util.Result;
import com.bracelet.vo.OverviewStatsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端「经营总览」统计 Controller
 * <p>由 AdminInterceptor 校验管理员身份
 */
@Slf4j
@RestController
@RequestMapping("/api/bracelet/admin")
@RequiredArgsConstructor
public class AdminOverviewController {

    private final OverviewService overviewService;

    /** 经营总览聚合统计（用户/订单/GMV/待办/7 日趋势/热门珠子） */
    @GetMapping("/overview/stats")
    public Result<OverviewStatsVO> stats() {
        try {
            return Result.ok(overviewService.stats());
        } catch (Exception e) {
            log.error("查询经营总览统计异常", e);
            return Result.fail("服务器内部错误");
        }
    }
}
