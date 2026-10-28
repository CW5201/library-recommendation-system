package com.library.recommendation.dto;

import jakarta.validation.constraints.NotNull;

public record ReasonRequest(
        @NotNull(message = "请选择图书") Long bookId,
        String algorithm
) {
}
