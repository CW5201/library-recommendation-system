package com.library.recommendation.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.library.recommendation.common.Result;
import com.library.recommendation.service.BookService;
import com.library.recommendation.service.RecommendationService;
import com.library.recommendation.vo.BookVO;
import com.library.recommendation.vo.RecommendationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final BookService bookService;

    @GetMapping("/hot")
    public Result<List<RecommendationVO>> hot(@RequestParam(defaultValue = "8") int limit) {
        return Result.ok(recommendationService.hot(limit, optionalUserId()));
    }

    @GetMapping("/new")
    public Result<List<BookVO>> newBooks(@RequestParam(defaultValue = "8") int limit) {
        return Result.ok(bookService.newBooks(limit, optionalUserId()));
    }

    @GetMapping("/personal")
    public Result<List<RecommendationVO>> personal(@RequestParam(defaultValue = "10") int limit) {
        return Result.ok(recommendationService.personal(StpUtil.getLoginIdAsLong(), limit));
    }

    @GetMapping("/similar/{bookId}")
    public Result<List<RecommendationVO>> similar(@PathVariable Long bookId,
                                                  @RequestParam(defaultValue = "6") int limit) {
        return Result.ok(recommendationService.similar(bookId, limit, optionalUserId()));
    }

    private Long optionalUserId() {
        return StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
    }
}
