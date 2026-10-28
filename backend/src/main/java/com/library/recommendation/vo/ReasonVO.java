package com.library.recommendation.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReasonVO {

    private String reason;
    private boolean aiGenerated;
}
