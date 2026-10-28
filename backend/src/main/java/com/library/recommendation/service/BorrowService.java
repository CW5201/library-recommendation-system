package com.library.recommendation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.recommendation.entity.Book;
import com.library.recommendation.entity.BorrowRecord;
import com.library.recommendation.entity.SysUser;
import com.library.recommendation.mapper.BookMapper;
import com.library.recommendation.mapper.BorrowRecordMapper;
import com.library.recommendation.mapper.SysUserMapper;
import com.library.recommendation.vo.BorrowRecordVO;
import com.library.recommendation.vo.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BorrowService {

    private final BorrowRecordMapper borrowMapper;
    private final BookMapper bookMapper;
    private final SysUserMapper userMapper;

    @Transactional
    public PageResult<BorrowRecordVO> page(long page, long size, String status, String keyword, Long userId) {
        borrowMapper.markOverdue();
        long safePage = Math.max(1, page);
        long safeSize = Math.min(50, Math.max(1, size));
        IPage<BorrowRecord> result = borrowMapper.selectBorrowPage(
                new Page<>(safePage, safeSize), userId, status, keyword);
        return PageResult.of(toVOs(result.getRecords()), result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Transactional
    public List<BorrowRecordVO> listMine(Long userId, String status) {
        borrowMapper.markOverdue();
        LambdaQueryWrapper<BorrowRecord> wrapper = new LambdaQueryWrapper<BorrowRecord>()
                .eq(BorrowRecord::getUserId, userId)
                .orderByDesc(BorrowRecord::getBorrowTime);
        if (status != null && !status.isBlank()) {
            wrapper.eq(BorrowRecord::getStatus, status);
        }
        return toVOs(borrowMapper.selectList(wrapper));
    }

    @Transactional
    public List<BorrowRecordVO> recent(int limit) {
        borrowMapper.markOverdue();
        List<BorrowRecord> records = borrowMapper.selectList(new LambdaQueryWrapper<BorrowRecord>()
                .orderByDesc(BorrowRecord::getBorrowTime)
                .last("LIMIT " + Math.max(1, Math.min(limit, 30))));
        return toVOs(records);
    }

    public long activeCount(Long userId) {
        LambdaQueryWrapper<BorrowRecord> wrapper = new LambdaQueryWrapper<BorrowRecord>()
                .in(BorrowRecord::getStatus, List.of("BORROWED", "OVERDUE"));
        if (userId != null) {
            wrapper.eq(BorrowRecord::getUserId, userId);
        }
        return borrowMapper.selectCount(wrapper);
    }

    public long todayCount() {
        return borrowMapper.selectCount(new LambdaQueryWrapper<BorrowRecord>()
                .ge(BorrowRecord::getBorrowTime, LocalDateTime.now().toLocalDate().atStartOfDay()));
    }

    public List<BorrowRecordVO> toVOs(List<BorrowRecord> records) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        Map<Long, Book> books = bookMapper.selectBatchIds(
                        records.stream().map(BorrowRecord::getBookId).distinct().toList())
                .stream().collect(Collectors.toMap(Book::getId, Function.identity(), (a, b) -> a));
        Map<Long, SysUser> users = userMapper.selectBatchIds(
                        records.stream().map(BorrowRecord::getUserId).distinct().toList())
                .stream().collect(Collectors.toMap(SysUser::getId, Function.identity(), (a, b) -> a));
        return records.stream().map(record -> {
            BorrowRecordVO vo = new BorrowRecordVO();
            vo.setId(record.getId());
            vo.setUserId(record.getUserId());
            vo.setBookId(record.getBookId());
            vo.setBorrowTime(record.getBorrowTime());
            vo.setDueTime(record.getDueTime());
            vo.setReturnTime(record.getReturnTime());
            vo.setStatus(record.getStatus());
            vo.setOverdue(!"RETURNED".equals(record.getStatus())
                    && record.getDueTime() != null && record.getDueTime().isBefore(LocalDateTime.now()));
            Book book = books.get(record.getBookId());
            if (book != null) {
                vo.setBookTitle(book.getTitle());
                vo.setBookAuthor(book.getAuthor());
                vo.setCover(book.getCover());
            }
            SysUser user = users.get(record.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setNickname(user.getNickname());
            }
            return vo;
        }).toList();
    }
}
