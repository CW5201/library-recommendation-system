package com.library.recommendation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank(message = "请输入想咨询的问题")
        @Size(max = 1000, message = "问题长度不能超过 1000 个字符")
        String message,
        @Size(max = 64, message = "会话标识过长")
        String sessionId
) {
}
