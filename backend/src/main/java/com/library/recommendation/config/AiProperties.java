package com.library.recommendation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    private boolean enabled = true;
    private String baseUrl = "https://api.deepseek.com/v1";
    private String apiKey = "";
    private String chatModel = "deepseek-chat";
    private String embeddingBaseUrl = "";
    private String embeddingApiKey = "";
    private String embeddingModel = "text-embedding-3-small";
    private int timeoutSeconds = 60;
    private double temperature = 0.3;
    private int maxContextBooks = 6;

    public boolean llmConfigured() {
        return enabled && apiKey != null && !apiKey.isBlank() && baseUrl != null && !baseUrl.isBlank();
    }

    public boolean embeddingApiConfigured() {
        return embeddingBaseUrl != null && !embeddingBaseUrl.isBlank()
                && embeddingModel != null && !embeddingModel.isBlank();
    }

    public String resolvedEmbeddingKey() {
        if (embeddingApiKey != null && !embeddingApiKey.isBlank()) {
            return embeddingApiKey;
        }
        return apiKey == null ? "" : apiKey;
    }
}
