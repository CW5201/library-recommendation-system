package com.library.recommendation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.recommendation.common.BusinessException;
import com.library.recommendation.config.AiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OpenAiClient {

    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public OpenAiClient(AiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeoutMs = Math.max(5, properties.getTimeoutSeconds()) * 1000;
        factory.setConnectTimeout(Math.min(timeoutMs, 15000));
        factory.setReadTimeout(timeoutMs);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public String chat(List<Map<String, String>> messages, int maxTokens) {
        if (!properties.llmConfigured()) {
            throw new BusinessException(503, "AI 大模型尚未配置");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getChatModel());
        body.put("messages", messages);
        body.put("temperature", properties.getTemperature());
        body.put("max_tokens", maxTokens);
        body.put("stream", false);

        try {
            String response = restClient.post()
                    .uri(join(properties.getBaseUrl(), "/chat/completions"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .body(body)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) {
                throw new BusinessException(502, "大模型返回内容为空，请稍后重试");
            }
            return content.asText().trim();
        } catch (BusinessException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            log.warn("LLM 请求失败: status={}, body={}", ex.getStatusCode().value(),
                    truncate(ex.getResponseBodyAsString()));
            throw new BusinessException(502, "大模型服务调用失败（HTTP " + ex.getStatusCode().value() + "），请检查 API 配置");
        } catch (Exception ex) {
            log.warn("LLM 请求异常: {}", ex.getMessage());
            throw new BusinessException(502, "大模型服务暂时不可用，请检查网络或接口地址");
        }
    }

    public List<List<Double>> embeddings(String baseUrl, String apiKey, String model, List<String> inputs) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("input", inputs);
        try {
            String response = restClient.post()
                    .uri(join(baseUrl, "/embeddings"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + (apiKey == null || apiKey.isBlank() ? "local" : apiKey))
                    .body(body)
                    .retrieve()
                    .body(String.class);
            JsonNode data = objectMapper.readTree(response).path("data");
            if (!data.isArray() || data.isEmpty()) {
                throw new BusinessException(502, "Embedding 接口返回为空");
            }
            List<List<Double>> vectors = new ArrayList<>();
            for (JsonNode item : data) {
                List<Double> vector = new ArrayList<>();
                for (JsonNode value : item.path("embedding")) {
                    vector.add(value.asDouble());
                }
                vectors.add(vector);
            }
            if (vectors.size() != inputs.size()) {
                throw new BusinessException(502, "Embedding 返回数量与输入不一致");
            }
            return vectors;
        } catch (BusinessException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            log.warn("Embedding 请求失败: status={}, body={}", ex.getStatusCode().value(),
                    truncate(ex.getResponseBodyAsString()));
            throw new BusinessException(502, "向量模型服务调用失败（HTTP " + ex.getStatusCode().value() + "）");
        } catch (Exception ex) {
            log.warn("Embedding 请求异常: {}", ex.getMessage());
            throw new BusinessException(502, "向量模型服务暂时不可用");
        }
    }

    private String join(String base, String path) {
        String normalized = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        return normalized + path;
    }

    private String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() > 300 ? value.substring(0, 300) : value;
    }
}
