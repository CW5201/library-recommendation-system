package com.library.recommendation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.recommendation.dto.NameValueDTO;
import com.library.recommendation.dto.TrendDTO;
import com.library.recommendation.entity.AiChatMessage;
import com.library.recommendation.entity.Book;
import com.library.recommendation.entity.BookCategory;
import com.library.recommendation.entity.BorrowRecord;
import com.library.recommendation.entity.SysUser;
import com.library.recommendation.entity.UserBehavior;
import com.library.recommendation.mapper.AiChatMessageMapper;
import com.library.recommendation.mapper.BookCategoryMapper;
import com.library.recommendation.mapper.BookMapper;
import com.library.recommendation.mapper.BorrowRecordMapper;
import com.library.recommendation.mapper.SysUserMapper;
import com.library.recommendation.mapper.UserBehaviorMapper;
import com.library.recommendation.vo.BookVO;
import com.library.recommendation.vo.DashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private final SysUserMapper userMapper;
    private final BookMapper bookMapper;
    private final BookCategoryMapper categoryMapper;
    private final BorrowRecordMapper borrowMapper;
    private final UserBehaviorMapper behaviorMapper;
    private final AiChatMessageMapper chatMapper;
    private final BorrowService borrowService;
    private final RecommendationService recommendationService;

    public DashboardVO overview() {
        DashboardVO vo = new DashboardVO();
        vo.setUserCount(count(userMapper.selectCount(null)));
        vo.setBookCount(count(bookMapper.selectCount(new LambdaQueryWrapper<Book>().eq(Book::getStatus, 1))));
        vo.setCategoryCount(count(categoryMapper.selectCount(null)));
        vo.setBorrowCount(count(borrowMapper.selectCount(null)));
        vo.setActiveBorrowCount(borrowService.activeCount(null));
        vo.setTodayBorrowCount(borrowService.todayCount());
        vo.setBehaviorCount(count(behaviorMapper.selectCount(null)));
        vo.setAiChatCount(count(chatMapper.selectCount(new LambdaQueryWrapper<AiChatMessage>()
                .eq(AiChatMessage::getRole, "user"))));
        vo.setRecentBorrows(borrowService.recent(8));
        List<BookVO> hot = new ArrayList<>();
        recommendationService.hot(8, null).forEach(item -> hot.add(item));
        vo.setHotBooks(hot);
        return vo;
    }

    public List<NameValueDTO> categoryStats() {
        return bookMapper.selectCategoryStats();
    }

    public List<NameValueDTO> behaviorStats() {
        return behaviorMapper.selectBehaviorStats();
    }

    public List<NameValueDTO> hotBooks(int limit) {
        return bookMapper.selectBorrowTop(Math.max(1, Math.min(limit, 20)));
    }

    public List<TrendDTO> borrowTrend(int days) {
        int safeDays = Math.max(7, Math.min(days, 60));
        LocalDate startDate = LocalDate.now().minusDays(safeDays - 1L);
        List<TrendDTO> rows = borrowMapper.selectBorrowTrend(startDate.atStartOfDay());
        Map<String, Long> values = rows.stream()
                .collect(Collectors.toMap(TrendDTO::getDate, TrendDTO::getValue, (a, b) -> a));
        List<TrendDTO> result = new ArrayList<>();
        for (int i = 0; i < safeDays; i++) {
            String date = startDate.plusDays(i).toString();
            TrendDTO point = new TrendDTO();
            point.setDate(date);
            point.setValue(values.getOrDefault(date, 0L));
            result.add(point);
        }
        return result;
    }

    public List<NameValueDTO> roleStats() {
        List<SysUser> users = userMapper.selectList(null);
        Map<String, Long> values = users.stream()
                .collect(Collectors.groupingBy(SysUser::getRole, Collectors.counting()));
        return values.entrySet().stream()
                .map(e -> new NameValueDTO(e.getKey(), e.getValue()))
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .toList();
    }

    public List<NameValueDTO> borrowStatusStats() {
        List<BorrowRecord> records = borrowMapper.selectList(null);
        Map<String, Long> values = records.stream()
                .collect(Collectors.groupingBy(BorrowRecord::getStatus, Collectors.counting()));
        return values.entrySet().stream()
                .map(e -> new NameValueDTO(e.getKey(), e.getValue()))
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .toList();
    }

    public long behaviorCountAfter(LocalDateTime time) {
        return behaviorMapper.selectCount(new LambdaQueryWrapper<UserBehavior>()
                .ge(UserBehavior::getCreateTime, time));
    }

    private long count(Long value) {
        return value == null ? 0 : value;
    }
}
