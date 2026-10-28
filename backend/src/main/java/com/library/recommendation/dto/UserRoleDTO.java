package com.library.recommendation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UserRoleDTO(
        @NotBlank(message = "请选择角色")
        @Pattern(regexp = "USER|ADMIN", message = "角色值不正确")
        String role
) {
}
