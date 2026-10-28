package com.library.recommendation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.recommendation.common.BusinessException;
import com.library.recommendation.entity.Book;
import com.library.recommendation.entity.BookRating;
import com.library.recommendation.entity.BorrowRecord;
import com.library.recommendation.entity.UserBehavior;
import com.library.recommendation.entity.UserFavorite;
import com.library.recommendation.mapper.BookMapper;
import com.library.recommendation.mapper.BookRatingMapper;
import com.library.recommendation.mapper.BorrowRecordMapper;
import com.library.recommendation.mapper.UserBehaviorMapper;
import com.library.recommendation.mapper.UserFavoriteMapper;
import com.library.recommendation.vo.BehaviorRecordVO;
import com.library.recommendation.vo.BookVO;
import com.library.recommendation.vo.FavoriteVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BehaviorService {

    private final UserBehaviorMapper behaviorMapper;
    private final UserFavoriteMapper favoriteMapper;
    private final BookRatingMapper ratingMapper;
    private final BorrowRecordMapper borrowMapper;
    private final BookMapper bookMapper;
    private final BookAssembler assembler;

    @Transactional
    public void recordView(Long userId, Long bookId) {
        requireBook(bookId);
        Long recent = behaviorMapper.selectCount(new LambdaQueryWrapper<UserBehavior>()
                .eq(UserBehavior::getUserId, userId)
                .eq(UserBehavior::getBookId, bookId)
                .eq(UserBehavior::getBehaviorType, "VIEW")
                .ge(UserBehavior::getCreateTime, LocalDateTime.now().minusMinutes(30)));
        if (recent != null && recent > 0) {
            return;
        }
        saveBehavior(userId, bookId, "VIEW", "浏览图书详情");
        bookMapper.incrementView(bookId);
    }

    @Transactional
    public boolean toggleFavorite(Long userId, Long bookId) {
        requireBook(bookId);
        UserFavorite exists = favoriteMapper.selectOne(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getBookId, bookId)
                .last("LIMIT 1"));
        if (exists != null) {
            favoriteMapper.deleteById(exists.getId());
            saveBehavior(userId, bookId, "UNFAVORITE", "取消收藏");
            return false;
        }
        UserFavorite favorite = new UserFavorite();
        favorite.setUserId(userId);
        favorite.setBookId(bookId);
        favorite.setCreateTime(LocalDateTime.now());
        favoriteMapper.insert(favorite);
        saveBehavior(userId, bookId, "FAVORITE", "收藏图书");
        return true;
    }

    @Transactional
    public BookRating rate(Long userId, Long bookId, int score) {
        requireBook(bookId);
        BookRating rating = ratingMapper.selectOne(new LambdaQueryWrapper<BookRating>()
                .eq(BookRating::getUserId, userId)
                .eq(BookRating::getBookId, bookId)
                .last("LIMIT 1"));
        if (rating == null) {
            rating = new BookRating();
            rating.setUserId(userId);
            rating.setBookId(bookId);
            rating.setScore(score);
            rating.setCreateTime(LocalDateTime.now());
            ratingMapper.insert(rating);
        } else {
            rating.setScore(score);
            ratingMapper.updateById(rating);
        }
        ratingMapper.refreshBookRating(bookId);
        saveBehavior(userId, bookId, "RATE", "评分 " + score + " 分");
        return rating;
    }

    @Transactional
    public BorrowRecord borrow(Long userId, Long bookId) {
        Book book = requireBook(bookId);
        if (book.getStatus() == null || book.getStatus() != 1) {
            throw new BusinessException("该图书已下架，暂不可借阅");
        }
        Long active = borrowMapper.selectCount(new LambdaQueryWrapper<BorrowRecord>()
                .eq(BorrowRecord::getUserId, userId)
                .eq(BorrowRecord::getBookId, bookId)
                .in(BorrowRecord::getStatus, List.of("BORROWED", "OVERDUE")));
        if (active != null && active > 0) {
            throw new BusinessException("你已借阅该图书，请归还后再借");
        }
        if (bookMapper.decreaseStock(bookId) == 0) {
            throw new BusinessException("该图书暂无可借库存");
        }
        LocalDateTime now = LocalDateTime.now();
        BorrowRecord record = new BorrowRecord();
        record.setUserId(userId);
        record.setBookId(bookId);
        record.setBorrowTime(now);
        record.setDueTime(now.plusDays(30));
        record.setStatus("BORROWED");
        record.setCreateTime(now);
        borrowMapper.insert(record);
        bookMapper.incrementBorrow(bookId);
        saveBehavior(userId, bookId, "BORROW", "借阅图书");
        return record;
    }

    @Transactional
    public void returnBook(Long recordId, Long userId, boolean admin) {
        BorrowRecord record = borrowMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(404, "借阅记录不存在");
        }
        if (!admin && !record.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该借阅记录");
        }
        if ("RETURNED".equals(record.getStatus())) {
            throw new BusinessException("该图书已归还");
        }
        record.setStatus("RETURNED");
        record.setReturnTime(LocalDateTime.now());
        borrowMapper.updateById(record);
        bookMapper.increaseStock(record.getBookId());
        saveBehavior(record.getUserId(), record.getBookId(), "RETURN", "归还图书");
    }

    public List<FavoriteVO> myFavorites(Long userId) {
        List<UserFavorite> favorites = favoriteMapper.selectList(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId)
                .orderByDesc(UserFavorite::getCreateTime));
        if (favorites.isEmpty()) {
            return List.of();
        }
        List<Long> bookIds = favorites.stream().map(UserFavorite::getBookId).toList();
        Map<Long, BookVO> books = assembler.toVOs(bookMapper.selectBatchIds(bookIds), userId).stream()
                .collect(Collectors.toMap(BookVO::getId, Function.identity(), (a, b) -> a));
        List<FavoriteVO> result = new ArrayList<>();
        for (UserFavorite favorite : favorites) {
            BookVO book = books.get(favorite.getBookId());
            if (book != null) {
                FavoriteVO vo = new FavoriteVO();
                vo.setBook(book);
                vo.setCreateTime(favorite.getCreateTime());
                result.add(vo);
            }
        }
        return result;
    }

    public List<BehaviorRecordVO> myHistory(Long userId, int limit) {
        List<UserBehavior> behaviors = behaviorMapper.selectList(new LambdaQueryWrapper<UserBehavior>()
                .eq(UserBehavior::getUserId, userId)
                .eq(UserBehavior::getBehaviorType, "VIEW")
                .orderByDesc(UserBehavior::getCreateTime)
                .last("LIMIT 200"));
        Map<Long, UserBehavior> latestByBook = new LinkedHashMap<>();
        for (UserBehavior behavior : behaviors) {
            latestByBook.putIfAbsent(behavior.getBookId(), behavior);
        }
        List<UserBehavior> distinct = new ArrayList<>(latestByBook.values());
        if (distinct.size() > Math.max(1, Math.min(limit, 100))) {
            distinct = distinct.subList(0, Math.max(1, Math.min(limit, 100)));
        }
        if (distinct.isEmpty()) {
            return List.of();
        }
        Map<Long, BookVO> books = assembler.toVOs(
                        bookMapper.selectBatchIds(distinct.stream().map(UserBehavior::getBookId).toList()), userId)
                .stream().collect(Collectors.toMap(BookVO::getId, Function.identity(), (a, b) -> a));
        List<BehaviorRecordVO> result = new ArrayList<>();
        for (UserBehavior behavior : distinct) {
            BookVO book = books.get(behavior.getBookId());
            if (book != null) {
                BehaviorRecordVO vo = new BehaviorRecordVO();
                vo.setId(behavior.getId());
                vo.setBehaviorType(behavior.getBehaviorType());
                vo.setRemark(behavior.getRemark());
                vo.setCreateTime(behavior.getCreateTime());
                vo.setBook(book);
                result.add(vo);
            }
        }
        return result;
    }

    public Integer myRating(Long userId, Long bookId) {
        BookRating rating = ratingMapper.selectOne(new LambdaQueryWrapper<BookRating>()
                .eq(BookRating::getUserId, userId)
                .eq(BookRating::getBookId, bookId)
                .last("LIMIT 1"));
        return rating == null ? null : rating.getScore();
    }

    private Book requireBook(Long bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException(404, "图书不存在");
        }
        return book;
    }

    private void saveBehavior(Long userId, Long bookId, String type, String remark) {
        UserBehavior behavior = new UserBehavior();
        behavior.setUserId(userId);
        behavior.setBookId(bookId);
        behavior.setBehaviorType(type);
        behavior.setRemark(remark);
        behavior.setCreateTime(LocalDateTime.now());
        behaviorMapper.insert(behavior);
    }
}
