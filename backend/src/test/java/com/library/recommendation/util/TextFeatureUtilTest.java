package com.library.recommendation.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TextFeatureUtilTest {

    @Test
    void similarChineseTextHasHigherCosine() {
        Map<String, Double> computer = TextFeatureUtil.vectorize("Java 编程 计算机 软件开发 面向对象");
        Map<String, Double> spring = TextFeatureUtil.vectorize("Spring Boot Java Web 开发 编程");
        Map<String, Double> novel = TextFeatureUtil.vectorize("长篇小说 文学 故事 人物 命运");
        double related = TextFeatureUtil.cosine(computer, spring);
        double unrelated = TextFeatureUtil.cosine(computer, novel);
        assertTrue(related > unrelated);
    }

    @Test
    void emptyTextReturnsZero() {
        assertTrue(TextFeatureUtil.vectorize("").isEmpty());
        assertTrue(TextFeatureUtil.cosine(Map.of(), Map.of()) == 0.0);
    }
}
