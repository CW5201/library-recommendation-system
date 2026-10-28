package com.library.recommendation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.recommendation.entity.BookRating;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface BookRatingMapper extends BaseMapper<BookRating> {

    @Update("""
            UPDATE book b
            SET b.rating_avg = (SELECT COALESCE(ROUND(AVG(r.score), 2), 0) FROM book_rating r WHERE r.book_id = #{bookId}),
                b.rating_count = (SELECT COUNT(*) FROM book_rating r WHERE r.book_id = #{bookId})
            WHERE b.id = #{bookId}
            """)
    int refreshBookRating(@Param("bookId") Long bookId);
}
