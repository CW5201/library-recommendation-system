package com.library.recommendation.util;

import com.library.recommendation.common.ScoreUtils;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScoreUtilsTest {

    @Test
    void maxNormalizeMapsScoresToZeroOne() {
        Map<Long, Double> raw = new LinkedHashMap<>();
        raw.put(1L, 10.0);
        raw.put(2L, 5.0);
        raw.put(3L, 0.0);
        Map<Long, Double> normalized = ScoreUtils.maxNormalize(raw);
        assertEquals(1.0, normalized.get(1L));
        assertEquals(0.5, normalized.get(2L));
        assertEquals(0.0, normalized.get(3L));
    }

    @Test
    void maxNormalizeHandlesEmptyAndZero() {
        assertEquals(0, ScoreUtils.maxNormalize(Map.of()).size());
        Map<Long, Double> raw = Map.of(1L, 0.0);
        assertEquals(0.0, ScoreUtils.maxNormalize(raw).get(1L));
    }

    @Test
    void clampAndRound() {
        assertEquals(0.0, ScoreUtils.clamp01(-1));
        assertEquals(1.0, ScoreUtils.clamp01(2));
        assertEquals(0.1235, ScoreUtils.round4(0.123456));
    }
}
