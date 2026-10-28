package com.library.recommendation.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.library.recommendation.common.Result;
import com.library.recommendation.dto.BookQuery;
import com.library.recommendation.dto.BookSaveDTO;
import com.library.recommendation.service.BookService;
import com.library.recommendation.vo.BookVO;
import com.library.recommendation.vo.PageResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/books")
@RequiredArgsConstructor
@SaCheckRole("ADMIN")
public class AdminBookController {

    private final BookService bookService;

    @GetMapping
    public Result<PageResult<BookVO>> page(@ModelAttribute BookQuery query) {
        return Result.ok(bookService.adminPage(query));
    }

    @PostMapping
    public Result<BookVO> create(@Valid @RequestBody BookSaveDTO dto) {
        return Result.ok("图书已新增", bookService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<BookVO> update(@PathVariable Long id, @Valid @RequestBody BookSaveDTO dto) {
        return Result.ok("图书已更新", bookService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        bookService.delete(id);
        return Result.ok("图书已删除", null);
    }
}
