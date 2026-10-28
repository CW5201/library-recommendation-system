package com.library.recommendation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileDTO(
        @NotBlank(message = "昵称不能为空")
        @Size(max = 50, message = "昵称不能超过 50 个字符")
        String nickname,
        @Email(message = "邮箱格式不正确")
        @Size(max = 100, message = "邮箱过长")
        String email,
        @Size(max = 20, message = "手机号过长")
        String phone,
        @Size(max = 255, message = "头像地址过长")
        String avatar
) {
}
