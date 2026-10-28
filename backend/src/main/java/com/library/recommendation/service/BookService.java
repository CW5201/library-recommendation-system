package com.library.recommendation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.recommendation.common.BusinessException;
import com.library.recommendation.dto.BookQuery;
import com.library.recommendation.dto.BookSaveDTO;
import com.library.recommendation.entity.Book;
import com.library.recommendation.entity.BookCategory;
import com.library.recommendation.entity.BorrowRecord;
import com.library.recommendation.mapper.BookCategoryMapper;
import com.library.recommendation.mapper.BookMapper;
import com.library.recommendation.mapper.BorrowRecordMapper;
import com.library.recommendation.vo.BookVO;
import com.library.recommendation.vo.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookMapper bookMapper;
    private final BookCategoryMapper categoryMapper;
    private final BorrowRecordMapper borrowMapper;
    private final BookAssembler assembler;

    public PageResult<BookVO> page(BookQuery query, Long userId) {
        Page<Book> page = buildPage(query, true);
        return PageResult.of(assembler.toVOs(page.getRecords(), userId), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public PageResult<BookVO> adminPage(BookQuery query) {
        Page<Book> page = buildPage(query, false);
        return PageResult.of(assembler.toVOs(page.getRecords(), null), page.getTotal(), page.getCurrent(), page.getSize());
    }

    private Page<Book> buildPage(BookQuery query, boolean onlyOnShelf) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        if (onlyOnShelf) {
            wrapper.eq(Book::getStatus, 1);
        } else if (query.getStatus() != null) {
            wrapper.eq(Book::getStatus, query.getStatus());
        }
        if (query.getCategoryId() != null) {
            wrapper.eq(Book::getCategoryId, query.getCategoryId());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(Book::getTitle, keyword)
                    .or().like(Book::getAuthor, keyword)
                    .or().like(Book::getIsbn, keyword)
                    .or().like(Book::getPublisher, keyword));
        }
        switch (query.getSort() == null ? "default" : query.getSort()) {
            case "hot" -> wrapper.orderByDesc(Book::getBorrowCount).orderByDesc(Book::getViewCount);
            case "rating" -> wrapper.orderByDesc(Book::getRatingAvg).orderByDesc(Book::getRatingCount);
            case "new" -> wrapper.orderByDesc(Book::getPublishDate).orderByDesc(Book::getId);
            case "title" -> wrapper.orderByAsc(Book::getTitle);
            default -> wrapper.orderByDesc(Book::getCreateTime).orderByDesc(Book::getId);
        }
        return bookMapper.selectPage(new Page<>(query.safePage(), query.safeSize()), wrapper);
    }

    public BookVO detail(Long id, Long userId) {
        Book book = getEntity(id);
        if (book.getStatus() == null || book.getStatus() != 1) {
            throw new BusinessException(404, "图书已下架");
        }
        return assembler.toVO(book, userId);
    }

    public Book getEntity(Long id) {
        Book book = bookMapper.selectById(id);
        if (book == null) {
            throw new BusinessException(404, "图书不存在");
        }
        return book;
    }

    public List<BookVO> newBooks(int limit, Long userId) {
        List<Book> books = bookMapper.selectList(new LambdaQueryWrapper<Book>()
                .eq(Book::getStatus, 1)
                .orderByDesc(Book::getPublishDate)
                .orderByDesc(Book::getCreateTime)
                .last("LIMIT " + Math.max(1, Math.min(limit, 30))));
        return assembler.toVOs(books, userId);
    }

    @Transactional
    public BookVO create(BookSaveDTO dto) {
        validate(dto, null);
        Book book = new Book();
        copy(dto, book);
        book.setRatingAvg(java.math.BigDecimal.ZERO);
        book.setRatingCount(0);
        book.setBorrowCount(0);
        book.setViewCount(0);
        bookMapper.insert(book);
        return assembler.toVO(book, null);
    }

    @Transactional
    public BookVO update(Long id, BookSaveDTO dto) {
        Book book = getEntity(id);
        validate(dto, id);
        copy(dto, book);
        bookMapper.updateById(book);
        return assembler.toVO(book, null);
    }

    @Transactional
    public void delete(Long id) {
        Book book = getEntity(id);
        Long borrowCount = borrowMapper.selectCount(new LambdaQueryWrapper<BorrowRecord>()
                .eq(BorrowRecord::getBookId, book.getId()));
        if (borrowCount != null && borrowCount > 0) {
            throw new BusinessException("该图书已有借阅记录，无法删除；可将图书状态改为下架");
        }
        bookMapper.deleteById(id);
    }

    private void validate(BookSaveDTO dto, Long excludeId) {
        BookCategory category = categoryMapper.selectById(dto.getCategoryId());
        if (category == null) {
            throw new BusinessException("所选分类不存在");
        }
        if (dto.getAvailableStock() > dto.getTotalStock()) {
            throw new BusinessException("可借数量不能大于馆藏总量");
        }
        LambdaQueryWrapper<Book> isbnQuery = new LambdaQueryWrapper<Book>().eq(Book::getIsbn, dto.getIsbn().trim());
        if (excludeId != null) {
            isbnQuery.ne(Book::getId, excludeId);
        }
        if (bookMapper.selectCount(isbnQuery) > 0) {
            throw new BusinessException("该 ISBN 已存在");
        }
        if (dto.getStatus() != null && dto.getStatus() != 0 && dto.getStatus() != 1) {
            throw new BusinessException("图书状态不正确");
        }
    }

    private void copy(BookSaveDTO dto, Book book) {
        book.setTitle(dto.getTitle().trim());
        book.setAuthor(dto.getAuthor().trim());
        book.setPublisher(StringUtils.hasText(dto.getPublisher()) ? dto.getPublisher().trim() : null);
        book.setIsbn(dto.getIsbn().trim());
        book.setCategoryId(dto.getCategoryId());
        book.setDescription(dto.getDescription());
        book.setCover(StringUtils.hasText(dto.getCover()) ? dto.getCover().trim() : "/covers/book.svg");
        book.setPublishDate(dto.getPublishDate());
        book.setTotalStock(dto.getTotalStock());
        book.setAvailableStock(dto.getAvailableStock());
        book.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
    }
}
