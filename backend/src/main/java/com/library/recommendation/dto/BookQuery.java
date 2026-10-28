package com.library.recommendation.dto;

import lombok.Data;

@Data
public class BookQuery {

    private long page = 1;
    private long size = 12;
    private String keyword;
    private Long categoryId;
    private Integer status;
    private String sort = "default";

    public long safePage() {
        return Math.max(1, page);
    }

    public long safeSize() {
        return Math.min(50, Math.max(1, size));
    }
}
