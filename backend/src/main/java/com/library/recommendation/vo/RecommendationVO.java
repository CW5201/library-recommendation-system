package com.library.recommendation.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RecommendationVO extends BookVO {

    private Double hotScore;
    private Double contentScore;
    private Double itemCfScore;
}
