package com.library.recommendation.service;

import com.library.recommendation.config.RecommendationProperties;
import com.library.recommendation.dto.InteractionDTO;
import com.library.recommendation.entity.Book;
import com.library.recommendation.entity.BookCategory;
import com.library.recommendation.entity.BorrowRecord;
import com.library.recommendation.entity.RecommendationLog;
import com.library.recommendation.entity.UserFavorite;
import com.library.recommendation.mapper.BookCategoryMapper;
import com.library.recommendation.mapper.BookMapper;
import com.library.recommendation.mapper.BorrowRecordMapper;
import com.library.recommendation.mapper.RecommendationLogMapper;
import com.library.recommendation.mapper.UserBehaviorMapper;
import com.library.recommendation.mapper.UserFavoriteMapper;
import com.library.recommendation.vo.BookVO;
import com.library.recommendation.vo.RecommendationVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecommendationServiceTest {

    @Mock
    private BookMapper bookMapper;
    @Mock
    private BookCategoryMapper categoryMapper;
    @Mock
    private UserBehaviorMapper behaviorMapper;
    @Mock
    private UserFavoriteMapper favoriteMapper;
    @Mock
    private BorrowRecordMapper borrowMapper;
    @Mock
    private RecommendationLogMapper logMapper;
    @Mock
    private BookAssembler assembler;

    private RecommendationService recommendationService;
    private List<Book> books;

    @BeforeEach
    void setUp() {
        recommendationService = new RecommendationService(bookMapper, categoryMapper, behaviorMapper,
                favoriteMapper, borrowMapper, logMapper, new RecommendationEngine(),
                new RecommendationProperties(), assembler);
        books = List.of(
                book(1L, "Java 编程", 1L, "Java 计算机 编程 软件开发", 100, 500, 4.9, 60),
                book(2L, "Spring 开发", 1L, "Java Spring Web 开发", 80, 400, 4.8, 50),
                book(3L, "算法导论", 1L, "算法 数据结构 计算机", 70, 350, 4.7, 40),
                book(4L, "文学小说", 2L, "文学 小说 故事", 60, 300, 4.6, 30),
                book(5L, "历史故事", 3L, "历史 人物 传记", 50, 250, 4.5, 20),
                book(6L, "心理学入门", 4L, "心理学 行为 情绪", 40, 200, 4.4, 10)
        );
        when(bookMapper.selectList(any())).thenReturn(books);
        when(bookMapper.selectBatchIds(any())).thenReturn(books);
        when(categoryMapper.selectList(any())).thenReturn(List.of(
                category(1L, "计算机科学"), category(2L, "文学小说"),
                category(3L, "历史传记"), category(4L, "心理学")));
        when(favoriteMapper.selectList(any())).thenReturn(List.of());
        when(borrowMapper.selectList(any())).thenReturn(List.of());
        when(assembler.toVOs(anyList(), any())).thenAnswer(invocation -> {
            List<Book> input = invocation.getArgument(0);
            List<BookVO> result = new ArrayList<>();
            for (Book book : input) {
                BookVO vo = BookVO.from(book);
                vo.setCategoryName("分类" + book.getCategoryId());
                result.add(vo);
            }
            return result;
        });
    }

    @Test
    void newUserGetsHotBooksAndNoRecommendationLog() {
        when(behaviorMapper.selectInteractions()).thenReturn(List.of());
        List<RecommendationVO> result = recommendationService.personal(1L, 5);
        assertFalse(result.isEmpty());
        assertTrue(result.get(0).getReason().contains("热门"));
        assertTrue(result.get(0).getHotScore() > 0);
        verify(logMapper, never()).insert(any(RecommendationLog.class));
    }

    @Test
    void personalRecommendationFiltersFavoritedAndBorrowedBooks() {
        when(behaviorMapper.selectInteractions()).thenReturn(List.of(
                interaction(1L, 1L, 5.0),
                interaction(1L, 3L, 4.0),
                interaction(2L, 1L, 5.0),
                interaction(2L, 2L, 4.0),
                interaction(3L, 3L, 5.0),
                interaction(3L, 2L, 4.0)));

        UserFavorite favorite = new UserFavorite();
        favorite.setUserId(1L);
        favorite.setBookId(2L);
        when(favoriteMapper.selectList(any())).thenReturn(List.of(favorite));

        BorrowRecord borrow = new BorrowRecord();
        borrow.setUserId(1L);
        borrow.setBookId(4L);
        borrow.setStatus("BORROWED");
        when(borrowMapper.selectList(any())).thenReturn(List.of(borrow));

        List<RecommendationVO> result = recommendationService.personal(1L, 5);
        assertFalse(result.isEmpty());
        assertTrue(result.stream().noneMatch(item -> item.getId().equals(2L)));
        assertTrue(result.stream().noneMatch(item -> item.getId().equals(4L)));
        verify(logMapper, org.mockito.Mockito.atLeastOnce()).insert(any(RecommendationLog.class));
    }

    @Test
    void recommendationReasonReferencesContentPreference() {
        when(behaviorMapper.selectInteractions()).thenReturn(List.of(
                interaction(1L, 1L, 5.0),
                interaction(1L, 2L, 4.0)));
        List<RecommendationVO> result = recommendationService.personal(1L, 3);
        assertTrue(result.stream().anyMatch(item -> item.getReason() != null
                && (item.getReason().contains("兴趣") || item.getReason().contains("同属")
                || item.getReason().contains("热门"))));
    }

    private Book book(Long id, String title, Long categoryId, String description,
                      int borrow, int view, double rating, int ratingCount) {
        Book book = new Book();
        book.setId(id);
        book.setTitle(title);
        book.setAuthor("作者" + id);
        book.setCategoryId(categoryId);
        book.setDescription(description);
        book.setBorrowCount(borrow);
        book.setViewCount(view);
        book.setRatingAvg(BigDecimal.valueOf(rating));
        book.setRatingCount(ratingCount);
        book.setTotalStock(10);
        book.setAvailableStock(5);
        book.setStatus(1);
        return book;
    }

    private BookCategory category(Long id, String name) {
        BookCategory category = new BookCategory();
        category.setId(id);
        category.setName(name);
        return category;
    }

    private InteractionDTO interaction(Long userId, Long bookId, Double weight) {
        InteractionDTO dto = new InteractionDTO();
        dto.setUserId(userId);
        dto.setBookId(bookId);
        dto.setWeight(weight);
        return dto;
    }
}
