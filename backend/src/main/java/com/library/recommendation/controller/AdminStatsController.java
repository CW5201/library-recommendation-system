package com.library.recommendation.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.library.recommendation.common.Result;
import com.library.recommendation.dto.NameValueDTO;
import com.library.recommendation.dto.TrendDTO;
import com.library.recommendation.service.AdminStatsService;
import com.library.recommendation.service.VectorIndexService;
import com.library.recommendation.vo.DashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@SaCheckRole("ADMIN")
public class AdminStatsController {

    private final AdminStatsService adminStatsService;
    private final VectorIndexService vectorIndexService;

    @GetMapping("/stats/overview")
    public Result<DashboardVO> overview() {
        return Result.ok(adminStatsService.overview());
    }

    @GetMapping("/stats/categories")
    public Result<List<NameValueDTO>> categories() {
        return Result.ok(adminStatsService.categoryStats());
    }

    @GetMapping("/stats/borrow-trend")
    public Result<List<TrendDTO>> borrowTrend(@RequestParam(defaultValue = "14") int days) {
        return Result.ok(adminStatsService.borrowTrend(days));
    }

    @GetMapping("/stats/behaviors")
    public Result<List<NameValueDTO>> behaviors() {
        return Result.ok(adminStatsService.behaviorStats());
    }

    @GetMapping("/stats/hot-books")
    public Result<List<NameValueDTO>> hotBooks(@RequestParam(defaultValue = "10") int limit) {
        return Result.ok(adminStatsService.hotBooks(limit));
    }

    @GetMapping("/stats/roles")
    public Result<List<NameValueDTO>> roles() {
        return Result.ok(adminStatsService.roleStats());
    }

    @GetMapping("/stats/borrow-status")
    public Result<List<NameValueDTO>> borrowStatus() {
        return Result.ok(adminStatsService.borrowStatusStats());
    }

    @PostMapping("/ai/rebuild-index")
    public Result<VectorIndexService.RebuildResult> rebuildIndex() {
        return Result.ok("向量索引重建完成", vectorIndexService.rebuildIndex());
    }
}
