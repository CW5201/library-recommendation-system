package com.library.recommendation.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.library.recommendation.common.Result;
import com.library.recommendation.dto.BookQuery;
import com.library.recommendation.service.BehaviorService;
import com.library.recommendation.service.BookService;
import com.library.recommendation.vo.BookVO;
import com.library.recommendation.vo.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final BehaviorService behaviorService;

    @GetMapping
    public Result<PageResult<BookVO>> page(@ModelAttribute BookQuery query) {
        return Result.ok(bookService.page(query, optionalUserId()));
    }

    @GetMapping("/{id}")
    public Result<BookVO> detail(@PathVariable Long id) {
        Long userId = optionalUserId();
        BookVO book = bookService.detail(id, userId);
        if (userId != null) {
            behaviorService.recordView(userId, id);
        }
        return Result.ok(book);
    }

    private Long optionalUserId() {
        return StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
    }
}
