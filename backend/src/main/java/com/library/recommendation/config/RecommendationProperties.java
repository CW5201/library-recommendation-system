package com.library.recommendation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "recommendation")
public class RecommendationProperties {

    private Weights weights = new Weights();

    @Data
    public static class Weights {
        private double hot = 0.35;
        private double content = 0.40;
        private double itemCf = 0.25;
    }
}
