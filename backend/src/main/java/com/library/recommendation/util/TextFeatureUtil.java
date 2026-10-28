package com.library.recommendation.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 轻量文本特征工具：英文按词、中文按字/二元组切分，用于内容推荐与相似图书计算。
 */
public final class TextFeatureUtil {

    private static final Pattern LATIN = Pattern.compile("[a-z0-9][a-z0-9+#._-]*");
    private static final Pattern CJK = Pattern.compile("[\\u4e00-\\u9fa5]+");

    private TextFeatureUtil() {
    }

    public static Map<String, Double> vectorize(String text) {
        Map<String, Double> counts = new HashMap<>();
        if (text == null || text.isBlank()) {
            return counts;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        Matcher latin = LATIN.matcher(lower);
        while (latin.find()) {
            add(counts, "w:" + latin.group(), 1.0);
        }
        Matcher cjk = CJK.matcher(lower);
        while (cjk.find()) {
            String sequence = cjk.group();
            for (int i = 0; i < sequence.length(); i++) {
                add(counts, "c:" + sequence.charAt(i), 0.35);
                if (i + 1 < sequence.length()) {
                    add(counts, "b:" + sequence.substring(i, i + 2), 1.0);
                }
            }
        }
        counts.replaceAll((key, value) -> 1.0 + Math.log(value));
        return counts;
    }

    public static double cosine(Map<String, Double> left, Map<String, Double> right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return 0.0;
        }
        Map<String, Double> small = left.size() <= right.size() ? left : right;
        Map<String, Double> large = small == left ? right : left;
        double dot = 0.0;
        for (Map.Entry<String, Double> entry : small.entrySet()) {
            Double other = large.get(entry.getKey());
            if (other != null) {
                dot += entry.getValue() * other;
            }
        }
        if (dot <= 0) {
            return 0.0;
        }
        return dot / (norm(left) * norm(right));
    }

    private static double norm(Map<String, Double> vector) {
        double sum = 0.0;
        for (Double value : vector.values()) {
            sum += value * value;
        }
        return Math.sqrt(sum);
    }

    private static void add(Map<String, Double> counts, String key, double weight) {
        counts.merge(key, weight, Double::sum);
    }
}
