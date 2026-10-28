package com.library.recommendation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UserStatusDTO(
        @NotNull(message = "请选择状态")
        @Min(value = 0, message = "状态值不正确")
        @Max(value = 1, message = "状态值不正确")
        Integer status
) {
}
