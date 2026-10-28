package com.library.recommendation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.recommendation.entity.Book;
import com.library.recommendation.entity.BookCategory;
import com.library.recommendation.entity.BookEmbedding;
import com.library.recommendation.mapper.BookCategoryMapper;
import com.library.recommendation.mapper.BookEmbeddingMapper;
import com.library.recommendation.mapper.BookMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorIndexService {

    private final BookMapper bookMapper;
    private final BookCategoryMapper categoryMapper;
    private final BookEmbeddingMapper embeddingMapper;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper;

    public synchronized RebuildResult rebuildIndex() {
        List<Book> books = bookMapper.selectList(new LambdaQueryWrapper<Book>().eq(Book::getStatus, 1));
        if (books.isEmpty()) {
            return new RebuildResult(0, embeddingService.configuredModelLabel());
        }
        Map<Long, String> categories = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(BookCategory::getId, BookCategory::getName, (a, b) -> a));
        List<String> texts = books.stream().map(book -> content(book, categories.get(book.getCategoryId()))).toList();
        EmbeddingService.EmbeddingBatch batch = embeddingService.embedDocuments(texts);

        embeddingMapper.delete(new LambdaQueryWrapper<>());
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < books.size(); i++) {
            Book book = books.get(i);
            List<Double> vector = batch.vectors().get(i);
            BookEmbedding embedding = new BookEmbedding();
            embedding.setBookId(book.getId());
            embedding.setModel(batch.model());
            embedding.setDimension(vector.size());
            embedding.setContentHash(hash(texts.get(i)));
            embedding.setUpdateTime(now);
            try {
                embedding.setVector(objectMapper.writeValueAsString(vector));
            } catch (Exception ex) {
                throw new IllegalStateException("图书向量序列化失败", ex);
            }
            embeddingMapper.insert(embedding);
        }
        log.info("图书向量索引重建完成，共 {} 本，模型 {}", books.size(), batch.model());
        return new RebuildResult(books.size(), batch.model());
    }

    public synchronized void ensureIndex() {
        List<Book> books = bookMapper.selectList(new LambdaQueryWrapper<Book>().eq(Book::getStatus, 1));
        List<BookEmbedding> embeddings = embeddingMapper.selectList(null);
        if (books.size() != embeddings.size()) {
            rebuildIndex();
            return;
        }
        String indexedModel = embeddings.get(0).getModel();
        if (indexedModel != null && indexedModel.startsWith("local-")
                && !indexedModel.equals(EmbeddingService.LOCAL_MODEL)) {
            rebuildIndex();
            return;
        }
        Map<Long, String> categories = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(BookCategory::getId, BookCategory::getName, (a, b) -> a));
        Map<Long, BookEmbedding> embeddingMap = embeddings.stream()
                .collect(Collectors.toMap(BookEmbedding::getBookId, Function.identity(), (a, b) -> a));
        for (Book book : books) {
            BookEmbedding embedding = embeddingMap.get(book.getId());
            if (embedding == null || !hash(content(book, categories.get(book.getCategoryId())))
                    .equals(embedding.getContentHash())) {
                rebuildIndex();
                return;
            }
        }
    }

    public List<SemanticHit> search(String query, int limit) {
        ensureIndex();
        List<Book> books = bookMapper.selectList(new LambdaQueryWrapper<Book>().eq(Book::getStatus, 1));
        Map<Long, Book> bookMap = books.stream().collect(Collectors.toMap(Book::getId, Function.identity()));
        List<BookEmbedding> embeddings = embeddingMapper.selectList(null);
        if (embeddings.isEmpty()) {
            return List.of();
        }
        String indexModel = embeddings.get(0).getModel();
        List<Double> queryVector;
        try {
            queryVector = embeddingService.embedQuery(query, indexModel);
        } catch (RuntimeException ex) {
            log.warn("向量检索失败，回退本地向量: {}", ex.getMessage());
            return localSearch(query, books, limit);
        }
        if (queryVector.isEmpty() || embeddings.get(0).getDimension() != queryVector.size()) {
            log.warn("索引维度与查询向量不一致，回退本地向量");
            return localSearch(query, books, limit);
        }
        List<SemanticHit> hits = new ArrayList<>();
        for (BookEmbedding embedding : embeddings) {
            Book book = bookMap.get(embedding.getBookId());
            if (book == null) {
                continue;
            }
            List<Double> vector = parseVector(embedding.getVector());
            if (vector.size() != queryVector.size()) {
                continue;
            }
            hits.add(new SemanticHit(book, cosine(queryVector, vector)));
        }
        return sorted(hits, limit);
    }

    public IndexStatus status() {
        List<BookEmbedding> embeddings = embeddingMapper.selectList(null);
        Long bookCount = bookMapper.selectCount(new LambdaQueryWrapper<Book>().eq(Book::getStatus, 1));
        String model = embeddings.isEmpty() ? embeddingService.configuredModelLabel() : embeddings.get(0).getModel();
        return new IndexStatus(embeddings.size(), bookCount == null ? 0 : bookCount, model);
    }

    private List<SemanticHit> localSearch(String query, List<Book> books, int limit) {
        List<Double> queryVector = embeddingService.localEmbed(query);
        Map<Long, String> categories = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(BookCategory::getId, BookCategory::getName, (a, b) -> a));
        List<SemanticHit> hits = new ArrayList<>();
        for (Book book : books) {
            List<Double> vector = embeddingService.localEmbed(content(book, categories.get(book.getCategoryId())));
            hits.add(new SemanticHit(book, cosine(queryVector, vector)));
        }
        return sorted(hits, limit);
    }

    private List<SemanticHit> sorted(List<SemanticHit> hits, int limit) {
        return hits.stream()
                .filter(hit -> hit.score() > 0)
                .sorted(Comparator.comparingDouble(SemanticHit::score).reversed())
                .limit(Math.max(1, Math.min(limit, 30)))
                .toList();
    }

    private List<Double> parseVector(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            log.warn("无法解析图书向量: {}", ex.getMessage());
            return List.of();
        }
    }

    private double cosine(List<Double> left, List<Double> right) {
        if (left.size() != right.size()) {
            return 0.0;
        }
        double dot = 0.0;
        double leftNorm = 0.0;
        double rightNorm = 0.0;
        for (int i = 0; i < left.size(); i++) {
            double l = left.get(i);
            double r = right.get(i);
            dot += l * r;
            leftNorm += l * l;
            rightNorm += r * r;
        }
        if (leftNorm == 0 || rightNorm == 0) {
            return 0.0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private String content(Book book, String categoryName) {
        Map<String, String> parts = new HashMap<>();
        parts.put("title", book.getTitle());
        parts.put("author", book.getAuthor());
        parts.put("publisher", book.getPublisher());
        parts.put("category", categoryName);
        parts.put("description", book.getDescription());
        return "书名：" + safe(parts.get("title")) + "；作者：" + safe(parts.get("author"))
                + "；出版社：" + safe(parts.get("publisher")) + "；分类：" + safe(parts.get("category"))
                + "；简介：" + safe(parts.get("description"));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String hash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("计算内容摘要失败", ex);
        }
    }

    public record SemanticHit(Book book, double score) {
    }

    public record RebuildResult(int count, String model) {
    }

    public record IndexStatus(int indexed, long activeBooks, String model) {
    }
}
