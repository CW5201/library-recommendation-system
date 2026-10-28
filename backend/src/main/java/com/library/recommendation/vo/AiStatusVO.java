package com.library.recommendation.vo;

import lombok.Data;

@Data
public class AiStatusVO {

    private boolean llmConfigured;
    private boolean embeddingApiConfigured;
    private String chatModel;
    private String embeddingModel;
    private String provider;
    private String message;
}
