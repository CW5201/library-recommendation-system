package com.library.recommendation.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.library.recommendation.common.Result;
import com.library.recommendation.service.BehaviorService;
import com.library.recommendation.service.BorrowService;
import com.library.recommendation.vo.BehaviorRecordVO;
import com.library.recommendation.vo.BorrowRecordVO;
import com.library.recommendation.vo.FavoriteVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class ProfileController {

    private final BehaviorService behaviorService;
    private final BorrowService borrowService;

    @GetMapping("/favorites")
    public Result<List<FavoriteVO>> favorites() {
        return Result.ok(behaviorService.myFavorites(StpUtil.getLoginIdAsLong()));
    }

    @GetMapping("/history")
    public Result<List<BehaviorRecordVO>> history(@RequestParam(defaultValue = "50") int limit) {
        return Result.ok(behaviorService.myHistory(StpUtil.getLoginIdAsLong(), limit));
    }

    @GetMapping("/borrows")
    public Result<List<BorrowRecordVO>> borrows(@RequestParam(required = false) String status) {
        return Result.ok(borrowService.listMine(StpUtil.getLoginIdAsLong(), status));
    }

    @GetMapping("/rating/{bookId}")
    public Result<Integer> rating(@PathVariable Long bookId) {
        return Result.ok(behaviorService.myRating(StpUtil.getLoginIdAsLong(), bookId));
    }
}
