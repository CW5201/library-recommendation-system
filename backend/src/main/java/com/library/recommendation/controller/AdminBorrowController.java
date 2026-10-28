package com.library.recommendation.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.library.recommendation.common.Result;
import com.library.recommendation.service.BehaviorService;
import com.library.recommendation.service.BorrowService;
import com.library.recommendation.vo.BorrowRecordVO;
import com.library.recommendation.vo.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/borrows")
@RequiredArgsConstructor
@SaCheckRole("ADMIN")
public class AdminBorrowController {

    private final BorrowService borrowService;
    private final BehaviorService behaviorService;

    @GetMapping
    public Result<PageResult<BorrowRecordVO>> page(@RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "10") long size,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) String keyword) {
        return Result.ok(borrowService.page(page, size, status, keyword, null));
    }

    @PutMapping("/{id}/return")
    public Result<Void> returnBook(@PathVariable Long id) {
        behaviorService.returnBook(id, StpUtil.getLoginIdAsLong(), true);
        return Result.ok("归还成功", null);
    }
}
