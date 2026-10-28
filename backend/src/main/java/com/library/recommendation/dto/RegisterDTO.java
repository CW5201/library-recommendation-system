package com.library.recommendation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterDTO(
        @NotBlank(message = "请输入用户名")
        @Pattern(regexp = "^[a-zA-Z0-9_]{3,20}$", message = "用户名需为 3-20 位字母、数字或下划线")
        String username,
        @NotBlank(message = "请输入密码")
        @Size(min = 6, max = 32, message = "密码长度需为 6-32 位")
        String password,
        @NotBlank(message = "请输入昵称")
        @Size(max = 50, message = "昵称不能超过 50 个字符")
        String nickname,
        @Email(message = "邮箱格式不正确")
        @Size(max = 100, message = "邮箱过长")
        String email
) {
}
