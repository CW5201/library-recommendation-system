package com.library.recommendation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.recommendation.dto.NameValueDTO;
import com.library.recommendation.entity.Book;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface BookMapper extends BaseMapper<Book> {

    @Update("UPDATE book SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementView(@Param("id") Long id);

    @Update("UPDATE book SET borrow_count = borrow_count + 1 WHERE id = #{id}")
    int incrementBorrow(@Param("id") Long id);

    @Update("UPDATE book SET available_stock = available_stock - 1 WHERE id = #{id} AND available_stock > 0")
    int decreaseStock(@Param("id") Long id);

    @Update("UPDATE book SET available_stock = available_stock + 1 WHERE id = #{id} AND available_stock < total_stock")
    int increaseStock(@Param("id") Long id);

    @Select("""
            SELECT c.name AS name, COUNT(b.id) AS `value`
            FROM book_category c
            LEFT JOIN book b ON b.category_id = c.id AND b.status = 1
            GROUP BY c.id, c.name
            ORDER BY `value` DESC, c.sort_order ASC
            """)
    List<NameValueDTO> selectCategoryStats();

    @Select("""
            SELECT title AS name, borrow_count AS `value`
            FROM book
            WHERE status = 1
            ORDER BY borrow_count DESC, rating_avg DESC
            LIMIT #{limit}
            """)
    List<NameValueDTO> selectBorrowTop(@Param("limit") int limit);
}
