package com.library.recommendation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.recommendation.entity.Book;
import com.library.recommendation.entity.BookCategory;
import com.library.recommendation.entity.BookRating;
import com.library.recommendation.entity.BorrowRecord;
import com.library.recommendation.entity.UserFavorite;
import com.library.recommendation.mapper.BookCategoryMapper;
import com.library.recommendation.mapper.BookRatingMapper;
import com.library.recommendation.mapper.BorrowRecordMapper;
import com.library.recommendation.mapper.UserFavoriteMapper;
import com.library.recommendation.vo.BookVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BookAssembler {

    private final BookCategoryMapper categoryMapper;
    private final UserFavoriteMapper favoriteMapper;
    private final BookRatingMapper ratingMapper;
    private final BorrowRecordMapper borrowMapper;

    public List<BookVO> toVOs(List<Book> books, Long userId) {
        if (books == null || books.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, String> categoryNames = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(BookCategory::getId, BookCategory::getName, (a, b) -> a));
        Map<Long, Boolean> favorites = new HashMap<>();
        Map<Long, Integer> ratings = new HashMap<>();
        Set<Long> borrowed = new java.util.HashSet<>();
        List<Long> bookIds = books.stream().map(Book::getId).toList();

        if (userId != null) {
            favoriteMapper.selectList(new LambdaQueryWrapper<UserFavorite>()
                            .eq(UserFavorite::getUserId, userId)
                            .in(UserFavorite::getBookId, bookIds))
                    .forEach(f -> favorites.put(f.getBookId(), true));
            ratingMapper.selectList(new LambdaQueryWrapper<BookRating>()
                            .eq(BookRating::getUserId, userId)
                            .in(BookRating::getBookId, bookIds))
                    .forEach(r -> ratings.put(r.getBookId(), r.getScore()));
            borrowMapper.selectList(new LambdaQueryWrapper<BorrowRecord>()
                            .eq(BorrowRecord::getUserId, userId)
                            .in(BorrowRecord::getBookId, bookIds)
                            .in(BorrowRecord::getStatus, List.of("BORROWED", "OVERDUE")))
                    .forEach(b -> borrowed.add(b.getBookId()));
        }

        return books.stream().map(book -> fill(book, categoryNames.get(book.getCategoryId()),
                favorites.getOrDefault(book.getId(), false),
                ratings.get(book.getId()),
                borrowed.contains(book.getId()))).toList();
    }

    public BookVO toVO(Book book, Long userId) {
        List<BookVO> list = toVOs(List.of(book), userId);
        return list.isEmpty() ? null : list.get(0);
    }

    private BookVO fill(Book book, String categoryName, boolean favorited, Integer myScore, boolean borrowed) {
        BookVO vo = BookVO.from(book);
        vo.setCategoryName(categoryName);
        vo.setFavorited(favorited);
        vo.setMyScore(myScore);
        vo.setBorrowed(borrowed);
        return vo;
    }
}
