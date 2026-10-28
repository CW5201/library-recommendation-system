package com.library.recommendation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SemanticSearchRequest(
        @NotBlank(message = "请输入检索内容")
        @Size(max = 300, message = "检索内容不能超过 300 个字符")
        String query,
        @Min(value = 1, message = "至少返回 1 条结果")
        @Max(value = 20, message = "最多返回 20 条结果")
        Integer limit
) {
    public int safeLimit() {
        return limit == null ? 8 : limit;
    }
}
