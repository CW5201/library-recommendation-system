package com.library.recommendation.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.library.recommendation.common.Result;
import com.library.recommendation.dto.CategorySaveDTO;
import com.library.recommendation.entity.BookCategory;
import com.library.recommendation.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
@SaCheckRole("ADMIN")
public class AdminCategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public Result<BookCategory> create(@Valid @RequestBody CategorySaveDTO dto) {
        return Result.ok("分类已新增", categoryService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<BookCategory> update(@PathVariable Long id, @Valid @RequestBody CategorySaveDTO dto) {
        return Result.ok("分类已更新", categoryService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.ok("分类已删除", null);
    }
}
