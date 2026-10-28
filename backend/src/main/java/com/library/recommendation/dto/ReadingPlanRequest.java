package com.library.recommendation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReadingPlanRequest(
        @NotBlank(message = "请描述你的学习目标")
        @Size(max = 300, message = "学习目标不能超过 300 个字符")
        String goal,
        @Min(value = 1, message = "阅读清单至少包含 1 本书")
        @Max(value = 10, message = "阅读清单最多包含 10 本书")
        Integer count
) {
    public int safeCount() {
        return count == null ? 5 : count;
    }
}
