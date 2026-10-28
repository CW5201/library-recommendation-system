package com.library.recommendation.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.library.recommendation.common.Result;
import com.library.recommendation.dto.RatingDTO;
import com.library.recommendation.entity.BorrowRecord;
import com.library.recommendation.service.BehaviorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/behaviors")
@RequiredArgsConstructor
public class BehaviorController {

    private final BehaviorService behaviorService;

    @PostMapping("/view/{bookId}")
    public Result<Void> view(@PathVariable Long bookId) {
        behaviorService.recordView(StpUtil.getLoginIdAsLong(), bookId);
        return Result.ok();
    }

    @PostMapping("/favorite/{bookId}")
    public Result<Map<String, Boolean>> favorite(@PathVariable Long bookId) {
        boolean favorited = behaviorService.toggleFavorite(StpUtil.getLoginIdAsLong(), bookId);
        return Result.ok(favorited ? "收藏成功" : "已取消收藏", Map.of("favorited", favorited));
    }

    @PostMapping("/rate/{bookId}")
    public Result<Map<String, Integer>> rate(@PathVariable Long bookId, @Valid @RequestBody RatingDTO dto) {
        behaviorService.rate(StpUtil.getLoginIdAsLong(), bookId, dto.score());
        return Result.ok("评分成功", Map.of("score", dto.score()));
    }

    @PostMapping("/borrow/{bookId}")
    public Result<BorrowRecord> borrow(@PathVariable Long bookId) {
        BorrowRecord record = behaviorService.borrow(StpUtil.getLoginIdAsLong(), bookId);
        return Result.ok("借阅成功，请在 30 天内归还", record);
    }

    @PostMapping("/return/{recordId}")
    public Result<Void> returnBook(@PathVariable Long recordId) {
        behaviorService.returnBook(recordId, StpUtil.getLoginIdAsLong(), false);
        return Result.ok("归还成功", null);
    }
}
