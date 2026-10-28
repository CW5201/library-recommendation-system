package com.library.recommendation.dto;

import lombok.Data;

@Data
public class InteractionDTO {

    private Long userId;
    private Long bookId;
    private Double weight;
}
