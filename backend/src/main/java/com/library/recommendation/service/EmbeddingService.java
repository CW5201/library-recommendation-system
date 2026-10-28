package com.library.recommendation.service;

import com.library.recommendation.config.AiProperties;
import com.library.recommendation.util.TextFeatureUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 向量服务：优先调用 OpenAI 兼容的 Embedding 接口，未配置或调用失败时回退到本地哈希向量。
 * 本地向量不依赖网络和第三方服务，保证语义检索流程在没有 API Key 时也能演示。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    public static final String LOCAL_MODEL = "local-hash-v2";
    private static final int LOCAL_DIMENSION = 384;

    private final AiProperties properties;
    private final OpenAiClient openAiClient;

    public EmbeddingBatch embedDocuments(List<String> texts) {
        if (properties.embeddingApiConfigured()) {
            try {
                List<List<Double>> vectors = openAiClient.embeddings(
                        properties.getEmbeddingBaseUrl(),
                        properties.resolvedEmbeddingKey(),
                        properties.getEmbeddingModel(),
                        texts);
                return new EmbeddingBatch(vectors, properties.getEmbeddingModel());
            } catch (RuntimeException ex) {
                log.warn("远程 Embedding 不可用，回退本地向量: {}", ex.getMessage());
            }
        }
        List<List<Double>> vectors = new ArrayList<>();
        for (String text : texts) {
            vectors.add(localEmbed(text));
        }
        return new EmbeddingBatch(vectors, LOCAL_MODEL);
    }

    public List<Double> embedQuery(String query, String indexModel) {
        if (indexModel != null && indexModel.startsWith("local-")) {
            return localEmbed(query);
        }
        if (properties.embeddingApiConfigured()) {
            return openAiClient.embeddings(
                    properties.getEmbeddingBaseUrl(),
                    properties.resolvedEmbeddingKey(),
                    properties.getEmbeddingModel(),
                    List.of(query)).get(0);
        }
        return localEmbed(query);
    }

    public List<Double> localEmbed(String text) {
        String expanded = expandSynonyms(text == null ? "" : text);
        Map<String, Double> features = TextFeatureUtil.vectorize(expanded);
        double[] vector = new double[LOCAL_DIMENSION];
        for (Map.Entry<String, Double> entry : features.entrySet()) {
            int index = Math.floorMod(entry.getKey().hashCode(), LOCAL_DIMENSION);
            vector[index] += entry.getValue();
        }
        double norm = 0.0;
        for (double value : vector) {
            norm += value * value;
        }
        norm = Math.sqrt(norm);
        List<Double> result = new ArrayList<>(LOCAL_DIMENSION);
        for (double value : vector) {
            result.add(norm == 0 ? 0.0 : value / norm);
        }
        return result;
    }

    public String configuredModelLabel() {
        return properties.embeddingApiConfigured() ? properties.getEmbeddingModel() : LOCAL_MODEL;
    }

    private String expandSynonyms(String text) {
        StringBuilder expanded = new StringBuilder(text);
        if (text.contains("人工智能") || text.contains("AI") || text.contains("机器学习")) {
            expanded.append(" artificial intelligence machine learning 神经网络 深度学习 算法");
        }
        if (text.contains("编程") || text.contains("开发") || text.contains("代码")) {
            expanded.append(" programming software java python 程序设计");
        }
        if (text.contains("小说") || text.contains("文学")) {
            expanded.append(" literature novel fiction 文学 故事");
        }
        if (text.contains("历史") || text.contains("传记")) {
            expanded.append(" history biography 历史 人物");
        }
        if (text.contains("心理")) {
            expanded.append(" psychology 心理 行为 情绪");
        }
        if (text.contains("经济") || text.contains("管理")) {
            expanded.append(" economics management business 经济 管理 商业");
        }
        if (text.contains("英语") || text.contains("外语")) {
            expanded.append(" english language learning 英语 语言");
        }
        return expanded.toString();
    }

    public record EmbeddingBatch(List<List<Double>> vectors, String model) {
    }
}
