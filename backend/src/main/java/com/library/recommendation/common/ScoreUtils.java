package com.library.recommendation.common;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 推荐分数工具：统一处理归一化、缺失值和加权融合。
 */
public final class ScoreUtils {

    private ScoreUtils() {
    }

    public static double clamp01(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

    /**
     * 最大值归一化，所有分数映射到 [0, 1]。
     * 全为 0 或空集合时返回 0 分。
     */
    public static Map<Long, Double> maxNormalize(Map<Long, Double> raw) {
        Map<Long, Double> result = new LinkedHashMap<>();
        if (raw == null || raw.isEmpty()) {
            return result;
        }
        double max = raw.values().stream()
                .filter(v -> v != null && !v.isNaN() && !v.isInfinite())
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);
        if (max <= 0.0) {
            raw.keySet().forEach(id -> result.put(id, 0.0));
            return result;
        }
        raw.forEach((id, value) -> {
            double safe = value == null || value.isNaN() || value.isInfinite() ? 0.0 : value;
            result.put(id, clamp01(safe / max));
        });
        return result;
    }

    public static double weightedSum(Map<Long, Double> scores, double weight) {
        return clamp01(scores.getOrDefault(0L, 0.0)) * clamp01(weight);
    }

    public static double weighted(double score, double weight) {
        return clamp01(score) * clamp01(weight);
    }

    public static double normalizeWeights(Collection<Double> weights) {
        double sum = weights.stream().filter(w -> w != null && w > 0).mapToDouble(Double::doubleValue).sum();
        return sum <= 0 ? 0 : sum;
    }

    public static double round4(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}
