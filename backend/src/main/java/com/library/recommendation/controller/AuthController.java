package com.library.recommendation.controller;

import com.library.recommendation.common.Result;
import com.library.recommendation.dto.ChangePasswordDTO;
import com.library.recommendation.dto.LoginDTO;
import com.library.recommendation.dto.RegisterDTO;
import com.library.recommendation.dto.UpdateProfileDTO;
import com.library.recommendation.service.AuthService;
import com.library.recommendation.vo.LoginVO;
import com.library.recommendation.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.ok("注册成功", authService.register(dto));
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok("登录成功", authService.login(dto));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok();
    }

    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.ok(authService.currentUser());
    }

    @PutMapping("/profile")
    public Result<UserVO> updateProfile(@Valid @RequestBody UpdateProfileDTO dto) {
        return Result.ok("资料已更新", authService.updateProfile(dto));
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        authService.changePassword(dto);
        return Result.ok("密码修改成功，请重新登录", null);
    }
}
