package com.library.recommendation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.library.recommendation.common.BusinessException;
import com.library.recommendation.dto.CategorySaveDTO;
import com.library.recommendation.entity.Book;
import com.library.recommendation.entity.BookCategory;
import com.library.recommendation.mapper.BookCategoryMapper;
import com.library.recommendation.mapper.BookMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final BookCategoryMapper categoryMapper;
    private final BookMapper bookMapper;

    public List<BookCategory> listAll() {
        return categoryMapper.selectList(new LambdaQueryWrapper<BookCategory>()
                .orderByAsc(BookCategory::getSortOrder)
                .orderByAsc(BookCategory::getId));
    }

    @Transactional
    public BookCategory create(CategorySaveDTO dto) {
        ensureNameUnique(dto.getName(), null);
        BookCategory category = new BookCategory();
        category.setName(dto.getName().trim());
        category.setDescription(dto.getDescription());
        category.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        categoryMapper.insert(category);
        return category;
    }

    @Transactional
    public BookCategory update(Long id, CategorySaveDTO dto) {
        BookCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(404, "分类不存在");
        }
        ensureNameUnique(dto.getName(), id);
        category.setName(dto.getName().trim());
        category.setDescription(dto.getDescription());
        category.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        categoryMapper.updateById(category);
        return category;
    }

    @Transactional
    public void delete(Long id) {
        BookCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(404, "分类不存在");
        }
        Long bookCount = bookMapper.selectCount(new LambdaQueryWrapper<Book>().eq(Book::getCategoryId, id));
        if (bookCount != null && bookCount > 0) {
            throw new BusinessException("该分类下仍有图书，无法删除");
        }
        categoryMapper.deleteById(id);
    }

    private void ensureNameUnique(String name, Long excludeId) {
        LambdaQueryWrapper<BookCategory> wrapper = new LambdaQueryWrapper<BookCategory>()
                .eq(BookCategory::getName, name.trim());
        if (excludeId != null) {
            wrapper.ne(BookCategory::getId, excludeId);
        }
        if (categoryMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("分类名称已存在");
        }
    }
}
