package com.library.recommendation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.recommendation.common.BusinessException;
import com.library.recommendation.config.RecommendationProperties;
import com.library.recommendation.dto.InteractionDTO;
import com.library.recommendation.entity.Book;
import com.library.recommendation.entity.BookCategory;
import com.library.recommendation.entity.BorrowRecord;
import com.library.recommendation.entity.RecommendationLog;
import com.library.recommendation.entity.UserBehavior;
import com.library.recommendation.entity.UserFavorite;
import com.library.recommendation.mapper.BookCategoryMapper;
import com.library.recommendation.mapper.BookMapper;
import com.library.recommendation.mapper.BorrowRecordMapper;
import com.library.recommendation.mapper.RecommendationLogMapper;
import com.library.recommendation.mapper.UserBehaviorMapper;
import com.library.recommendation.mapper.UserFavoriteMapper;
import com.library.recommendation.vo.BookVO;
import com.library.recommendation.vo.RecommendationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final BookMapper bookMapper;
    private final BookCategoryMapper categoryMapper;
    private final UserBehaviorMapper behaviorMapper;
    private final UserFavoriteMapper favoriteMapper;
    private final BorrowRecordMapper borrowMapper;
    private final RecommendationLogMapper logMapper;
    private final RecommendationEngine engine;
    private final RecommendationProperties properties;
    private final BookAssembler assembler;

    public List<RecommendationVO> hot(int limit, Long userId) {
        List<Book> books = activeBooks();
        Map<Long, Double> hot = engine.hotScores(books);
        Map<Long, Book> bookMap = books.stream().collect(Collectors.toMap(Book::getId, Function.identity()));
        List<ScoredBook> ranked = hot.entrySet().stream()
                .map(e -> new ScoredBook(e.getKey(), e.getValue(), e.getValue(), 0.0, 0.0, "HOT"))
                .sorted(Comparator.comparingDouble(ScoredBook::score).reversed())
                .limit(Math.max(1, Math.min(limit, 30)))
                .toList();
        return toRecommendationVOs(ranked, bookMap, userId, null);
    }

    @Transactional
    public List<RecommendationVO> personal(Long userId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 30));
        List<Book> books = activeBooks();
        Map<Long, Book> bookMap = books.stream().collect(Collectors.toMap(Book::getId, Function.identity()));
        Map<Long, String> categoryNames = categoryNames();
        List<InteractionDTO> allInteractions = behaviorMapper.selectInteractions();
        List<InteractionDTO> userInteractions = allInteractions.stream()
                .filter(i -> i.getUserId() != null && i.getUserId().equals(userId))
                .toList();
        Set<Long> excluded = excludedBookIds(userId);

        List<ScoredBook> ranked;
        boolean coldStart = userInteractions.isEmpty();
        if (coldStart) {
            ranked = engine.coldStart(books, safeLimit);
        } else {
            Map<Long, Double> hot = engine.hotScores(books);
            Map<Long, Double> content = engine.contentScores(books, categoryNames, userInteractions);
            Map<Long, Double> itemCf = engine.itemCfScores(allInteractions, userId);
            RecommendationProperties.Weights weights = properties.getWeights();
            ranked = engine.fuse(books, hot, content, itemCf, excluded,
                    weights.getHot(), weights.getContent(), weights.getItemCf(), safeLimit);
            if (ranked.isEmpty()) {
                ranked = engine.coldStart(books.stream()
                        .filter(book -> !excluded.contains(book.getId())).toList(), safeLimit);
            }
        }

        List<RecommendationVO> result = toRecommendationVOs(ranked, bookMap, userId, userInteractions);
        if (result.size() < safeLimit) {
            Set<Long> selected = result.stream().map(BookVO::getId).collect(Collectors.toCollection(HashSet::new));
            selected.addAll(excluded);
            List<Book> fallback = books.stream().filter(book -> !selected.contains(book.getId())).toList();
            List<ScoredBook> extra = engine.coldStart(fallback, safeLimit - result.size());
            result.addAll(toRecommendationVOs(extra, bookMap, userId, userInteractions));
        }
        if (!coldStart) {
            saveLogs(userId, result);
        }
        return result;
    }

    public List<RecommendationVO> similar(Long bookId, int limit, Long userId) {
        Book target = bookMapper.selectById(bookId);
        if (target == null) {
            throw new BusinessException(404, "图书不存在");
        }
        List<Book> books = activeBooks().stream().filter(book -> !book.getId().equals(bookId)).toList();
        Map<Long, Book> bookMap = books.stream().collect(Collectors.toMap(Book::getId, Function.identity()));
        Map<Long, Double> scores = engine.similarScores(target, books, categoryNames(), behaviorMapper.selectInteractions());
        List<ScoredBook> ranked = scores.entrySet().stream()
                .map(e -> new ScoredBook(e.getKey(), e.getValue(), 0.0, e.getValue(), 0.0, "SIMILAR"))
                .sorted(Comparator.comparingDouble(ScoredBook::score).reversed())
                .limit(Math.max(1, Math.min(limit, 20)))
                .toList();
        List<RecommendationVO> result = toRecommendationVOs(ranked, bookMap, userId, null);
        String category = categoryNames().get(target.getCategoryId());
        for (RecommendationVO vo : result) {
            vo.setReason("与《" + target.getTitle() + "》" +
                    (category != null && category.equals(vo.getCategoryName())
                            ? "同属" + category + "分类，主题相近"
                            : "在主题和内容上具有相似性"));
            vo.setAlgorithm("SIMILAR");
        }
        return result;
    }

    public String reasonFor(Long bookId, Long userId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException(404, "图书不存在");
        }
        Map<Long, String> categoryNames = categoryNames();
        List<InteractionDTO> interactions = behaviorMapper.selectInteractions().stream()
                .filter(i -> i.getUserId() != null && i.getUserId().equals(userId))
                .toList();
        Map<Long, Double> userItems = engine.userItemWeights(interactions);
        if (userItems.isEmpty()) {
            return hotReason(book);
        }
        List<Book> interactedBooks = bookMapper.selectBatchIds(userItems.keySet()).stream().toList();
        String category = categoryNames.get(book.getCategoryId());
        for (Book interacted : interactedBooks) {
            if (book.getCategoryId().equals(interacted.getCategoryId())) {
                return "与你关注的《" + interacted.getTitle() + "》同属" + category + "分类，主题相近";
            }
        }
        Book source = interactedBooks.stream()
                .max(Comparator.comparingDouble(b -> userItems.getOrDefault(b.getId(), 0.0)))
                .orElse(null);
        if (source != null) {
            return "结合你近期浏览的《" + source.getTitle() + "》，为你推荐同主题的《" + book.getTitle() + "》";
        }
        return hotReason(book);
    }

    private List<RecommendationVO> toRecommendationVOs(List<ScoredBook> ranked,
                                                       Map<Long, Book> bookMap,
                                                       Long userId,
                                                       List<InteractionDTO> userInteractions) {
        if (ranked.isEmpty()) {
            return new ArrayList<>();
        }
        List<Book> books = ranked.stream().map(s -> bookMap.get(s.bookId()))
                .filter(java.util.Objects::nonNull).toList();
        Map<Long, BookVO> base = assembler.toVOs(books, userId).stream()
                .collect(Collectors.toMap(BookVO::getId, Function.identity()));
        Map<Long, String> categoryNames = categoryNames();
        List<RecommendationVO> result = new ArrayList<>();
        for (ScoredBook scored : ranked) {
            Book book = bookMap.get(scored.bookId());
            BookVO bookVO = base.get(scored.bookId());
            if (book == null || bookVO == null) {
                continue;
            }
            RecommendationVO vo = new RecommendationVO();
            org.springframework.beans.BeanUtils.copyProperties(bookVO, vo);
            vo.setRecommendationScore(scored.score());
            vo.setHotScore(scored.hotScore());
            vo.setContentScore(scored.contentScore());
            vo.setItemCfScore(scored.itemCfScore());
            vo.setAlgorithm(scored.source());
            vo.setReason(buildReason(book, scored.source(), categoryNames, userInteractions, userId));
            result.add(vo);
        }
        return result;
    }

    private String buildReason(Book book, String source, Map<Long, String> categoryNames,
                               List<InteractionDTO> userInteractions, Long userId) {
        if ("CONTENT".equals(source)) {
            String category = categoryNames.get(book.getCategoryId());
            List<InteractionDTO> interactions = userInteractions == null
                    ? List.of() : userInteractions;
            Set<Long> ids = interactions.stream().map(InteractionDTO::getBookId).collect(Collectors.toSet());
            if (!ids.isEmpty()) {
                List<Book> interacted = bookMapper.selectBatchIds(ids).stream().toList();
                Book sameCategory = interacted.stream()
                        .filter(b -> book.getCategoryId().equals(b.getCategoryId()))
                        .max(Comparator.comparing(b -> interactions.stream()
                                .filter(i -> i.getBookId().equals(b.getId()))
                                .mapToDouble(i -> i.getWeight() == null ? 0 : i.getWeight()).sum()))
                        .orElse(null);
                if (sameCategory != null) {
                    return "与你关注的《" + sameCategory.getTitle() + "》同属" + category + "分类，内容主题相近";
                }
            }
            return "根据你的浏览与收藏偏好，这本书与你的阅读兴趣匹配";
        }
        if ("ITEM_CF".equals(source)) {
            return "喜欢同类图书的读者也经常借阅这本书";
        }
        return hotReason(book);
    }

    private String hotReason(Book book) {
        String rating = book.getRatingAvg() == null ? "暂无" : book.getRatingAvg().toPlainString();
        return "近期借阅 " + (book.getBorrowCount() == null ? 0 : book.getBorrowCount())
                + " 次，读者评分 " + rating + " 分，是当前热门图书";
    }

    private Set<Long> excludedBookIds(Long userId) {
        Set<Long> excluded = new HashSet<>();
        favoriteMapper.selectList(new LambdaQueryWrapper<UserFavorite>().eq(UserFavorite::getUserId, userId))
                .forEach(f -> excluded.add(f.getBookId()));
        borrowMapper.selectList(new LambdaQueryWrapper<BorrowRecord>().eq(BorrowRecord::getUserId, userId))
                .forEach(b -> excluded.add(b.getBookId()));
        return excluded;
    }

    private List<Book> activeBooks() {
        return bookMapper.selectList(new LambdaQueryWrapper<Book>().eq(Book::getStatus, 1));
    }

    private Map<Long, String> categoryNames() {
        return categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(BookCategory::getId, BookCategory::getName, (a, b) -> a));
    }

    private void saveLogs(Long userId, List<RecommendationVO> recommendations) {
        LocalDateTime now = LocalDateTime.now();
        for (RecommendationVO vo : recommendations) {
            RecommendationLog log = new RecommendationLog();
            log.setUserId(userId);
            log.setBookId(vo.getId());
            log.setAlgorithm(vo.getAlgorithm() == null ? "HYBRID" : vo.getAlgorithm());
            log.setScore(BigDecimal.valueOf(vo.getRecommendationScore() == null ? 0 : vo.getRecommendationScore()));
            log.setReason(vo.getReason());
            log.setCreateTime(now);
            logMapper.insert(log);
        }
    }
}
