package com.library.recommendation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.recommendation.common.BusinessException;
import com.library.recommendation.config.AiProperties;
import com.library.recommendation.dto.ChatRequest;
import com.library.recommendation.dto.ReadingPlanRequest;
import com.library.recommendation.dto.SemanticSearchRequest;
import com.library.recommendation.entity.AiChatMessage;
import com.library.recommendation.entity.Book;
import com.library.recommendation.entity.BookCategory;
import com.library.recommendation.mapper.AiChatMessageMapper;
import com.library.recommendation.mapper.BookCategoryMapper;
import com.library.recommendation.mapper.BookMapper;
import com.library.recommendation.vo.AiStatusVO;
import com.library.recommendation.vo.BookVO;
import com.library.recommendation.vo.ChatMessageVO;
import com.library.recommendation.vo.ChatVO;
import com.library.recommendation.vo.ReadingPlanVO;
import com.library.recommendation.vo.ReasonVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final AiProperties properties;
    private final OpenAiClient openAiClient;
    private final VectorIndexService vectorIndexService;
    private final RagPromptBuilder promptBuilder;
    private final BookAssembler assembler;
    private final BookMapper bookMapper;
    private final BookCategoryMapper categoryMapper;
    private final AiChatMessageMapper chatMessageMapper;
    private final RecommendationService recommendationService;
    private final ObjectMapper objectMapper;

    public AiStatusVO status() {
        VectorIndexService.IndexStatus indexStatus = vectorIndexService.status();
        AiStatusVO vo = new AiStatusVO();
        vo.setLlmConfigured(properties.llmConfigured());
        vo.setEmbeddingApiConfigured(properties.embeddingApiConfigured());
        vo.setChatModel(properties.getChatModel());
        vo.setEmbeddingModel(indexStatus.model());
        vo.setProvider(indexStatus.model() != null && indexStatus.model().startsWith("local-")
                ? "本地哈希向量（离线兜底）" : "OpenAI 兼容 Embedding");
        if (properties.llmConfigured()) {
            vo.setMessage("AI 大模型已配置；语义检索索引 " + indexStatus.indexed() + " 本图书，"
                    + "馆藏可检索 " + indexStatus.activeBooks() + " 本。");
        } else {
            vo.setMessage("未配置大模型 API Key。AI 助手会展示数据库检索结果和配置提示，"
                    + "图书管理、借阅和传统推荐功能不受影响。");
        }
        return vo;
    }

    public ChatVO chat(Long userId, ChatRequest request) {
        String sessionId = request.sessionId() == null || request.sessionId().isBlank()
                ? UUID.randomUUID().toString() : request.sessionId();
        AiChatMessage userMessage = saveMessage(userId, sessionId, "user", request.message(), null);
        List<VectorIndexService.SemanticHit> hits = retrieve(request.message(),
                Math.max(1, Math.min(properties.getMaxContextBooks(), 10)));
        List<Book> books = hits.stream().map(VectorIndexService.SemanticHit::book).toList();
        List<BookVO> relatedBooks = toBookVOs(books, userId, hits);
        String context = promptBuilder.buildContext(books, categoryNames());

        ChatVO vo = new ChatVO();
        vo.setSessionId(sessionId);
        vo.setRelatedBooks(relatedBooks);
        vo.setAiConfigured(properties.llmConfigured());

        String answer;
        if (!properties.llmConfigured()) {
            answer = fallbackAnswer(request.message(), relatedBooks, false, null);
            vo.setFallback(true);
            vo.setNotice("大模型 API Key 未配置，当前展示数据库检索结果");
        } else {
            try {
                List<Map<String, String>> messages = new ArrayList<>();
                messages.add(message("system", promptBuilder.systemPrompt()));
                messages.addAll(historyForPrompt(userId, sessionId, userMessage.getId()));
                messages.add(message("user", promptBuilder.chatUserPrompt(request.message(), context)));
                answer = openAiClient.chat(messages, 1400);
            } catch (RuntimeException ex) {
                log.warn("AI 对话调用失败，返回检索结果: {}", ex.getMessage());
                answer = fallbackAnswer(request.message(), relatedBooks, true, ex.getMessage());
                vo.setFallback(true);
                vo.setNotice(ex.getMessage());
            }
        }
        vo.setAnswer(answer);
        saveMessage(userId, sessionId, "assistant", answer, relatedBooks);
        return vo;
    }

    public List<BookVO> semanticSearch(Long userId, SemanticSearchRequest request) {
        List<VectorIndexService.SemanticHit> hits = retrieve(request.query(), request.safeLimit());
        if (hits.isEmpty()) {
            List<Book> keywordBooks = keywordSearch(request.query(), request.safeLimit());
            hits = keywordBooks.stream()
                    .map(book -> new VectorIndexService.SemanticHit(book, 0.0)).toList();
        }
        return toBookVOs(hits.stream().map(VectorIndexService.SemanticHit::book).toList(), userId, hits);
    }

    public ReadingPlanVO readingPlan(Long userId, ReadingPlanRequest request) {
        int count = request.safeCount();
        List<VectorIndexService.SemanticHit> hits = retrieve(request.goal(), count + 4);
        List<Book> books = hits.stream().map(VectorIndexService.SemanticHit::book)
                .limit(count).toList();
        ReadingPlanVO vo = new ReadingPlanVO();
        vo.setBooks(toBookVOs(books, userId, hits));
        if (books.isEmpty()) {
            vo.setContent("当前馆藏中没有检索到与该学习目标相关的图书。请尝试换一个关键词，或联系图书馆采购。");
            vo.setAiGenerated(false);
            return vo;
        }
        String context = promptBuilder.buildContext(books, categoryNames());
        if (properties.llmConfigured()) {
            try {
                List<Map<String, String>> messages = List.of(
                        message("system", promptBuilder.systemPrompt()),
                        message("user", promptBuilder.readingPlanPrompt(request.goal(), count, context)));
                vo.setContent(openAiClient.chat(messages, 1600));
                vo.setAiGenerated(true);
                return vo;
            } catch (RuntimeException ex) {
                log.warn("AI 阅读清单生成失败，使用本地结果: {}", ex.getMessage());
            }
        }
        vo.setContent(localReadingPlan(request.goal(), books));
        vo.setAiGenerated(false);
        return vo;
    }

    public ReasonVO recommendReason(Long userId, Long bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException(404, "图书不存在");
        }
        String factReason = recommendationService.reasonFor(bookId, userId);
        if (properties.llmConfigured()) {
            try {
                String category = categoryNames().getOrDefault(book.getCategoryId(), "综合");
                List<Map<String, String>> messages = List.of(
                        message("system", promptBuilder.systemPrompt()),
                        message("user", promptBuilder.reasonPrompt(book.getTitle(), book.getAuthor(), category, factReason)));
                String answer = openAiClient.chat(messages, 200);
                return new ReasonVO(answer, true);
            } catch (RuntimeException ex) {
                log.warn("AI 推荐理由生成失败，使用规则理由: {}", ex.getMessage());
            }
        }
        return new ReasonVO(factReason, false);
    }

    public List<ChatMessageVO> history(Long userId, String sessionId) {
        LambdaQueryWrapper<AiChatMessage> wrapper = new LambdaQueryWrapper<AiChatMessage>()
                .eq(AiChatMessage::getUserId, userId)
                .orderByDesc(AiChatMessage::getCreateTime)
                .orderByDesc(AiChatMessage::getId)
                .last("LIMIT 50");
        if (sessionId != null && !sessionId.isBlank()) {
            wrapper.eq(AiChatMessage::getSessionId, sessionId);
        }
        List<AiChatMessage> messages = chatMessageMapper.selectList(wrapper);
        Collections.reverse(messages);
        return messages.stream().map(this::toChatMessageVO).toList();
    }

    private List<VectorIndexService.SemanticHit> retrieve(String query, int limit) {
        try {
            return vectorIndexService.search(query, limit);
        } catch (RuntimeException ex) {
            log.warn("语义检索失败，回退关键词检索: {}", ex.getMessage());
            return keywordSearch(query, limit).stream()
                    .map(book -> new VectorIndexService.SemanticHit(book, 0.0)).toList();
        }
    }

    private List<Book> keywordSearch(String query, int limit) {
        String keyword = query == null ? "" : query.trim();
        if (keyword.isEmpty()) {
            return List.of();
        }
        return bookMapper.selectList(new LambdaQueryWrapper<Book>()
                .eq(Book::getStatus, 1)
                .and(w -> w.like(Book::getTitle, keyword)
                        .or().like(Book::getAuthor, keyword)
                        .or().like(Book::getDescription, keyword))
                .orderByDesc(Book::getBorrowCount)
                .last("LIMIT " + Math.max(1, Math.min(limit, 30))));
    }

    private List<BookVO> toBookVOs(List<Book> books, Long userId, List<VectorIndexService.SemanticHit> hits) {
        if (books.isEmpty()) {
            return List.of();
        }
        Map<Long, Double> scores = hits.stream().collect(Collectors.toMap(
                hit -> hit.book().getId(), VectorIndexService.SemanticHit::score, (a, b) -> Math.max(a, b)));
        List<BookVO> result = assembler.toVOs(books, userId);
        for (BookVO vo : result) {
            Double score = scores.get(vo.getId());
            if (score != null && score > 0) {
                vo.setSimilarity(Math.round(score * 10000.0) / 10000.0);
                vo.setReason("语义相关度 " + String.format("%.2f", Math.min(1.0, score)));
            }
        }
        return result;
    }

    private String fallbackAnswer(String question, List<BookVO> books, boolean aiFailed, String error) {
        StringBuilder answer = new StringBuilder();
        if (books.isEmpty()) {
            answer.append("当前馆藏中没有检索到与“").append(question).append("”相关的图书。");
        } else {
            answer.append("我根据问题在馆藏数据库中检索到以下相关图书：\n");
            for (int i = 0; i < Math.min(books.size(), 5); i++) {
                BookVO book = books.get(i);
                answer.append(i + 1).append(". 《").append(book.getTitle()).append("》")
                        .append("，作者：").append(book.getAuthor())
                        .append("，馆藏可借 ").append(book.getAvailableStock())
                        .append(" 本。\n");
            }
        }
        if (aiFailed) {
            answer.append("\n大模型服务暂时不可用");
            if (error != null && !error.isBlank()) {
                answer.append("（").append(error).append("）");
            }
            answer.append("，以上内容为数据库检索结果，未经过大模型改写。");
        } else {
            answer.append("\n当前未配置大模型 API Key。请在后端 .env 中配置 AI_BASE_URL、AI_API_KEY、AI_CHAT_MODEL，")
                    .append("重启后端后即可获得大模型生成的 RAG 回答。");
        }
        return answer.toString();
    }

    private String localReadingPlan(String goal, List<Book> books) {
        StringBuilder content = new StringBuilder("根据你的目标“").append(goal)
                .append("”，从馆藏中整理了以下阅读清单：\n");
        int index = 1;
        for (Book book : books) {
            content.append(index++).append(". 《").append(book.getTitle()).append("》")
                    .append("（").append(book.getAuthor()).append("）");
            if (book.getDescription() != null && !book.getDescription().isBlank()) {
                content.append("：").append(book.getDescription(), 0, Math.min(80, book.getDescription().length()));
                if (book.getDescription().length() > 80) {
                    content.append("...");
                }
            }
            content.append("\n");
        }
        content.append("\n建议按顺序阅读，并结合馆藏可借数量安排借阅。配置 AI 大模型后可获得更详细的阅读计划。");
        return content.toString();
    }

    private List<Map<String, String>> historyForPrompt(Long userId, String sessionId, Long excludeId) {
        List<AiChatMessage> messages = chatMessageMapper.selectList(new LambdaQueryWrapper<AiChatMessage>()
                .eq(AiChatMessage::getUserId, userId)
                .eq(AiChatMessage::getSessionId, sessionId)
                .lt(AiChatMessage::getId, excludeId)
                .orderByDesc(AiChatMessage::getId)
                .last("LIMIT 6"));
        Collections.reverse(messages);
        return messages.stream().map(m -> message(m.getRole(), m.getContent())).toList();
    }

    private AiChatMessage saveMessage(Long userId, String sessionId, String role, String content, List<BookVO> relatedBooks) {
        AiChatMessage message = new AiChatMessage();
        message.setUserId(userId);
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setCreateTime(LocalDateTime.now());
        if (relatedBooks != null) {
            try {
                message.setRelatedBooks(objectMapper.writeValueAsString(relatedBooks));
            } catch (Exception ex) {
                log.warn("关联图书序列化失败: {}", ex.getMessage());
            }
        }
        chatMessageMapper.insert(message);
        return message;
    }

    private ChatMessageVO toChatMessageVO(AiChatMessage message) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(message.getId());
        vo.setRole(message.getRole());
        vo.setContent(message.getContent());
        vo.setCreateTime(message.getCreateTime());
        if (message.getRelatedBooks() != null && !message.getRelatedBooks().isBlank()) {
            try {
                vo.setRelatedBooks(objectMapper.readValue(message.getRelatedBooks(), new TypeReference<>() {
                }));
            } catch (Exception ex) {
                vo.setRelatedBooks(List.of());
            }
        }
        return vo;
    }

    private Map<Long, String> categoryNames() {
        return categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(BookCategory::getId, BookCategory::getName, (a, b) -> a));
    }

    private Map<String, String> message(String role, String content) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("role", role);
        map.put("content", content);
        return map;
    }
}
