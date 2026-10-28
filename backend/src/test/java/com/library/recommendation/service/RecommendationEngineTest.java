package com.library.recommendation.service;

import com.library.recommendation.dto.InteractionDTO;
import com.library.recommendation.entity.Book;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommendationEngineTest {

    private final RecommendationEngine engine = new RecommendationEngine();

    @Test
    void hotScorePrefersHighBorrowAndRating() {
        Book hot = book(1L, "热门计算机书", "作者A", 1L,
                "Java 编程 软件开发 计算机 算法", 200, 1000, 4.9, 120);
        Book cold = book(2L, "冷门小说", "作者B", 2L,
                "文学 小说 故事", 2, 10, 3.0, 2);
        Map<Long, Double> scores = engine.hotScores(List.of(hot, cold));
        assertTrue(scores.get(1L) > scores.get(2L));
        assertTrue(scores.get(1L) <= 1.0);
    }

    @Test
    void contentScoreMatchesUserCategory() {
        Book java = book(1L, "Java 编程核心", "作者A", 1L,
                "Java 面向对象 编程 软件开发 计算机", 50, 200, 4.8, 30);
        Book spring = book(2L, "Spring Boot 开发", "作者B", 1L,
                "Java Web Spring 编程 软件开发", 40, 160, 4.7, 20);
        Book novel = book(3L, "长篇小说", "作者C", 2L,
                "文学 小说 故事 人物 命运", 60, 300, 4.6, 25);
        Map<Long, String> categories = Map.of(1L, "计算机科学", 2L, "文学小说");
        List<InteractionDTO> interactions = List.of(interaction(10L, 1L, 5.0));
        Map<Long, Double> scores = engine.contentScores(List.of(java, spring, novel), categories, interactions);
        assertTrue(scores.get(2L) > scores.get(3L));
    }

    @Test
    void itemCfRecommendsCoViewedBook() {
        Book first = book(1L, "算法导论", "作者A", 1L, "算法 计算机", 10, 20, 4.5, 5);
        Book second = book(2L, "数据结构", "作者B", 1L, "数据结构 算法", 10, 20, 4.6, 5);
        Book third = book(3L, "历史", "作者C", 3L, "历史 人物", 10, 20, 4.4, 5);
        List<InteractionDTO> interactions = new ArrayList<>();
        interactions.add(interaction(10L, 1L, 5.0));
        interactions.add(interaction(10L, 2L, 4.0));
        interactions.add(interaction(20L, 1L, 5.0));
        interactions.add(interaction(20L, 2L, 4.0));
        interactions.add(interaction(30L, 1L, 5.0));
        Map<Long, Double> scores = engine.itemCfScores(interactions, 30L);
        assertTrue(scores.getOrDefault(2L, 0.0) > 0);
        assertFalse(scores.containsKey(1L));
        assertFalse(scores.containsKey(3L));
        assertTrue(scores.get(2L) > 0);
    }

    @Test
    void similarBooksPreferSameCategory() {
        Book target = book(1L, "Java 编程", "作者A", 1L, "Java 计算机 软件开发", 10, 20, 4.5, 5);
        Book sameCategory = book(2L, "Spring 开发", "作者B", 1L, "Java Spring 软件开发", 10, 20, 4.5, 5);
        Book other = book(3L, "艺术的故事", "作者C", 7L, "艺术 设计 绘画", 10, 20, 4.5, 5);
        List<InteractionDTO> interactions = List.of(
                interaction(10L, 1L, 5.0), interaction(10L, 2L, 5.0),
                interaction(20L, 1L, 5.0), interaction(20L, 2L, 5.0));
        Map<Long, Double> scores = engine.similarScores(target, List.of(sameCategory, other),
                Map.of(1L, "计算机科学", 7L, "艺术设计"), interactions);
        assertTrue(scores.get(2L) > scores.get(3L));
    }

    @Test
    void fuseFiltersExcludedBooksAndCombinesWeights() {
        Book first = book(1L, "书一", "作者A", 1L, "计算机 编程", 100, 500, 4.8, 50);
        Book second = book(2L, "书二", "作者B", 1L, "计算机 算法", 80, 400, 4.7, 40);
        Book third = book(3L, "书三", "作者C", 2L, "文学 小说", 50, 200, 4.5, 20);
        Map<Long, Double> hot = engine.hotScores(List.of(first, second, third));
        Map<Long, Double> content = Map.of(1L, 1.0, 2L, 0.8, 3L, 0.1);
        Map<Long, Double> cf = Map.of(1L, 0.6, 2L, 0.9, 3L, 0.0);
        List<ScoredBook> result = engine.fuse(List.of(first, second, third), hot, content, cf,
                Set.of(1L), 0.35, 0.40, 0.25, 5);
        assertEquals(2, result.size());
        assertTrue(result.stream().noneMatch(scored -> scored.bookId().equals(1L)));
        assertEquals(2L, result.get(0).bookId());
        assertTrue(result.get(0).score() > 0);
    }

    @Test
    void coldStartUsesHotBooks() {
        Book hot = book(1L, "热门", "作者A", 1L, "计算机", 100, 500, 4.9, 80);
        Book cold = book(2L, "冷门", "作者B", 2L, "文学", 1, 2, 3.0, 1);
        List<ScoredBook> result = engine.coldStart(List.of(hot, cold), 2);
        assertEquals(1L, result.get(0).bookId());
        assertEquals("HOT", result.get(0).source());
    }

    private Book book(Long id, String title, String author, Long categoryId, String description,
                      int borrowCount, int viewCount, double ratingAvg, int ratingCount) {
        Book book = new Book();
        book.setId(id);
        book.setTitle(title);
        book.setAuthor(author);
        book.setCategoryId(categoryId);
        book.setDescription(description);
        book.setBorrowCount(borrowCount);
        book.setViewCount(viewCount);
        book.setRatingAvg(BigDecimal.valueOf(ratingAvg));
        book.setRatingCount(ratingCount);
        book.setTotalStock(10);
        book.setAvailableStock(5);
        book.setStatus(1);
        return book;
    }

    private InteractionDTO interaction(Long userId, Long bookId, Double weight) {
        InteractionDTO dto = new InteractionDTO();
        dto.setUserId(userId);
        dto.setBookId(bookId);
        dto.setWeight(weight);
        return dto;
    }
}
