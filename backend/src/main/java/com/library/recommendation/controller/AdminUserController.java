package com.library.recommendation.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.library.recommendation.common.Result;
import com.library.recommendation.dto.UserRoleDTO;
import com.library.recommendation.dto.UserStatusDTO;
import com.library.recommendation.service.AdminUserService;
import com.library.recommendation.vo.PageResult;
import com.library.recommendation.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@SaCheckRole("ADMIN")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public Result<PageResult<UserVO>> page(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String role,
                                           @RequestParam(required = false) Integer status) {
        return Result.ok(adminUserService.page(page, size, keyword, role, status));
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody UserStatusDTO dto) {
        adminUserService.updateStatus(id, dto.status());
        return Result.ok("用户状态已更新", null);
    }

    @PutMapping("/{id}/role")
    public Result<Void> updateRole(@PathVariable Long id, @Valid @RequestBody UserRoleDTO dto) {
        adminUserService.updateRole(id, dto.role());
        return Result.ok("用户角色已更新", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminUserService.delete(id);
        return Result.ok("用户已删除", null);
    }
}
