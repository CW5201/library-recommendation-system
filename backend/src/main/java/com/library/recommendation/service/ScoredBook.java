package com.library.recommendation.service;

public record ScoredBook(
        Long bookId,
        double score,
        double hotScore,
        double contentScore,
        double itemCfScore,
        String source
) {
}
