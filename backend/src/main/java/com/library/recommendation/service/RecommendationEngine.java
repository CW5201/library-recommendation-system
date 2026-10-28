package com.library.recommendation.service;

import com.library.recommendation.common.ScoreUtils;
import com.library.recommendation.dto.InteractionDTO;
import com.library.recommendation.entity.Book;
import com.library.recommendation.util.TextFeatureUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 推荐算法核心。所有算法先输出原始分，再统一最大值归一化到 [0,1] 后融合。
 */
@Component
public class RecommendationEngine {

    public Map<Long, Double> hotScores(List<Book> books) {
        Map<Long, Double> raw = new LinkedHashMap<>();
        for (Book book : books) {
            double borrow = Math.log1p(safe(book.getBorrowCount()));
            double view = Math.log1p(safe(book.getViewCount()));
            double rating = (book.getRatingAvg() == null ? 0.0 : book.getRatingAvg().doubleValue()) / 5.0;
            double ratingCount = Math.log1p(safe(book.getRatingCount()));
            double stock = book.getTotalStock() != null && book.getTotalStock() > 0
                    ? (double) safe(book.getAvailableStock()) / book.getTotalStock() : 0.0;
            double score = 0.35 * borrow + 0.22 * view + 0.23 * rating + 0.15 * ratingCount + 0.05 * stock;
            raw.put(book.getId(), score);
        }
        return ScoreUtils.maxNormalize(raw);
    }

    public Map<Long, Double> contentScores(List<Book> books,
                                           Map<Long, String> categoryNames,
                                           List<InteractionDTO> interactions) {
        Map<Long, Book> bookMap = new HashMap<>();
        Map<Long, Map<String, Double>> features = new HashMap<>();
        for (Book book : books) {
            bookMap.put(book.getId(), book);
            features.put(book.getId(), bookFeatures(book, categoryNames.get(book.getCategoryId())));
        }

        Map<Long, Double> userItemWeights = userItemWeights(interactions);
        if (userItemWeights.isEmpty()) {
            return ScoreUtils.maxNormalize(new HashMap<>());
        }
        Map<String, Double> profile = new HashMap<>();
        Map<Long, Double> categoryWeights = new HashMap<>();
        double totalWeight = 0.0;
        for (Map.Entry<Long, Double> entry : userItemWeights.entrySet()) {
            Book interacted = bookMap.get(entry.getKey());
            if (interacted == null) {
                continue;
            }
            double weight = entry.getValue();
            totalWeight += weight;
            Map<String, Double> vector = features.get(interacted.getId());
            if (vector != null) {
                vector.forEach((term, value) -> profile.merge(term, value * weight, Double::sum));
            }
            categoryWeights.merge(interacted.getCategoryId(), weight, Double::sum);
        }

        Map<Long, Double> raw = new LinkedHashMap<>();
        for (Book book : books) {
            if (userItemWeights.containsKey(book.getId())) {
                continue;
            }
            double textSimilarity = TextFeatureUtil.cosine(profile, features.get(book.getId()));
            double categoryAffinity = totalWeight <= 0 ? 0
                    : categoryWeights.getOrDefault(book.getCategoryId(), 0.0) / totalWeight;
            raw.put(book.getId(), 0.7 * textSimilarity + 0.3 * categoryAffinity);
        }
        return ScoreUtils.maxNormalize(raw);
    }

    public Map<Long, Double> itemCfScores(List<InteractionDTO> allInteractions, Long userId) {
        Map<Long, Map<Long, Double>> itemUsers = itemUserVectors(allInteractions);
        Map<Long, Double> userItems = userItemWeights(allInteractions.stream()
                .filter(i -> i.getUserId() != null && i.getUserId().equals(userId)).toList());
        if (userItems.isEmpty()) {
            return new LinkedHashMap<>();
        }
        Map<Long, Double> raw = new LinkedHashMap<>();
        for (Long candidate : itemUsers.keySet()) {
            if (userItems.containsKey(candidate)) {
                continue;
            }
            double weighted = 0.0;
            double similaritySum = 0.0;
            for (Map.Entry<Long, Double> history : userItems.entrySet()) {
                Map<Long, Double> source = itemUsers.get(history.getKey());
                double similarity = cosineMap(source, itemUsers.get(candidate));
                if (similarity > 0) {
                    weighted += similarity * history.getValue();
                    similaritySum += similarity;
                }
            }
            if (similaritySum > 0) {
                raw.put(candidate, weighted / similaritySum);
            }
        }
        return ScoreUtils.maxNormalize(raw);
    }

    public Map<Long, Double> similarScores(Book target,
                                           List<Book> books,
                                           Map<Long, String> categoryNames,
                                           List<InteractionDTO> interactions) {
        Map<String, Double> targetFeatures = bookFeatures(target, categoryNames.get(target.getCategoryId()));
        Map<Long, Double> contentRaw = new LinkedHashMap<>();
        for (Book book : books) {
            if (book.getId().equals(target.getId())) {
                continue;
            }
            contentRaw.put(book.getId(),
                    TextFeatureUtil.cosine(targetFeatures, bookFeatures(book, categoryNames.get(book.getCategoryId()))));
        }
        Map<Long, Double> content = ScoreUtils.maxNormalize(contentRaw);

        Map<Long, Map<Long, Double>> itemUsers = itemUserVectors(interactions);
        Map<Long, Double> cfRaw = new LinkedHashMap<>();
        for (Book book : books) {
            if (book.getId().equals(target.getId())) {
                continue;
            }
            cfRaw.put(book.getId(), cosineMap(itemUsers.get(target.getId()), itemUsers.get(book.getId())));
        }
        Map<Long, Double> cf = ScoreUtils.maxNormalize(cfRaw);

        Map<Long, Double> fused = new LinkedHashMap<>();
        Set<Long> candidates = new HashSet<>();
        candidates.addAll(content.keySet());
        candidates.addAll(cf.keySet());
        for (Long id : candidates) {
            fused.put(id, 0.65 * content.getOrDefault(id, 0.0) + 0.35 * cf.getOrDefault(id, 0.0));
        }
        return ScoreUtils.maxNormalize(fused);
    }

    public List<ScoredBook> fuse(List<Book> books,
                                 Map<Long, Double> hot,
                                 Map<Long, Double> content,
                                 Map<Long, Double> itemCf,
                                 Set<Long> excluded,
                                 double hotWeight,
                                 double contentWeight,
                                 double itemCfWeight,
                                 int limit) {
        Set<Long> safeExcluded = excluded == null ? Set.of() : excluded;
        List<ScoredBook> candidates = new ArrayList<>();
        for (Book book : books) {
            if (safeExcluded.contains(book.getId())) {
                continue;
            }
            double h = hot.getOrDefault(book.getId(), 0.0);
            double c = content.getOrDefault(book.getId(), 0.0);
            double i = itemCf.getOrDefault(book.getId(), 0.0);
            double score = hotWeight * h + contentWeight * c + itemCfWeight * i;
            if (score <= 0) {
                continue;
            }
            String source;
            if (contentWeight * c >= hotWeight * h && contentWeight * c >= itemCfWeight * i) {
                source = "CONTENT";
            } else if (itemCfWeight * i >= hotWeight * h) {
                source = "ITEM_CF";
            } else {
                source = "HOT";
            }
            candidates.add(new ScoredBook(book.getId(), ScoreUtils.round4(score), h, c, i, source));
        }
        candidates.sort(Comparator.comparingDouble(ScoredBook::score).reversed()
                .thenComparing(book -> hot.getOrDefault(book.bookId(), 0.0), Comparator.reverseOrder()));
        return candidates.size() > limit ? candidates.subList(0, limit) : candidates;
    }

    public List<ScoredBook> coldStart(List<Book> books, int limit) {
        Map<Long, Double> hot = hotScores(books);
        return books.stream()
                .filter(book -> hot.getOrDefault(book.getId(), 0.0) > 0)
                .map(book -> new ScoredBook(book.getId(), hot.get(book.getId()),
                        hot.get(book.getId()), 0.0, 0.0, "HOT"))
                .sorted(Comparator.comparingDouble(ScoredBook::score).reversed())
                .limit(limit)
                .toList();
    }

    public Map<Long, Double> userItemWeights(List<InteractionDTO> interactions) {
        Map<Long, Double> result = new LinkedHashMap<>();
        if (interactions == null) {
            return result;
        }
        for (InteractionDTO interaction : interactions) {
            if (interaction.getBookId() != null && interaction.getWeight() != null) {
                result.merge(interaction.getBookId(), interaction.getWeight(), Double::sum);
            }
        }
        return result;
    }

    private Map<Long, Map<Long, Double>> itemUserVectors(List<InteractionDTO> interactions) {
        Map<Long, Map<Long, Double>> itemUsers = new HashMap<>();
        if (interactions == null) {
            return itemUsers;
        }
        for (InteractionDTO interaction : interactions) {
            if (interaction.getBookId() == null || interaction.getUserId() == null || interaction.getWeight() == null) {
                continue;
            }
            itemUsers.computeIfAbsent(interaction.getBookId(), key -> new HashMap<>())
                    .merge(interaction.getUserId(), interaction.getWeight(), Double::sum);
        }
        return itemUsers;
    }

    private double cosineMap(Map<Long, Double> left, Map<Long, Double> right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return 0.0;
        }
        Map<Long, Double> small = left.size() <= right.size() ? left : right;
        Map<Long, Double> large = small == left ? right : left;
        double dot = 0.0;
        for (Map.Entry<Long, Double> entry : small.entrySet()) {
            Double other = large.get(entry.getKey());
            if (other != null) {
                dot += entry.getValue() * other;
            }
        }
        if (dot <= 0) {
            return 0.0;
        }
        double leftNorm = Math.sqrt(left.values().stream().mapToDouble(v -> v * v).sum());
        double rightNorm = Math.sqrt(right.values().stream().mapToDouble(v -> v * v).sum());
        return leftNorm == 0 || rightNorm == 0 ? 0.0 : dot / (leftNorm * rightNorm);
    }

    private Map<String, Double> bookFeatures(Book book, String categoryName) {
        StringBuilder text = new StringBuilder();
        append(text, book.getTitle(), 3);
        append(text, book.getAuthor(), 2);
        append(text, categoryName, 4);
        append(text, book.getPublisher(), 1);
        append(text, book.getDescription(), 1);
        return TextFeatureUtil.vectorize(text.toString());
    }

    private void append(StringBuilder builder, String value, int repeat) {
        if (value == null || value.isBlank()) {
            return;
        }
        for (int i = 0; i < repeat; i++) {
            builder.append(value).append(' ');
        }
    }

    private int safe(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }
}
