package com.library.recommendation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginDTO(
        @NotBlank(message = "请输入用户名") String username,
        @NotBlank(message = "请输入密码") @Size(max = 64, message = "密码长度不正确") String password
) {
}
