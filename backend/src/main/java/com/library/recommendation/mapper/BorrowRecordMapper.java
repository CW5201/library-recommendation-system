package com.library.recommendation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.recommendation.dto.TrendDTO;
import com.library.recommendation.entity.BorrowRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BorrowRecordMapper extends BaseMapper<BorrowRecord> {

    @Update("UPDATE borrow_record SET status = 'OVERDUE' WHERE status = 'BORROWED' AND due_time < NOW()")
    int markOverdue();

    @Select("""
            SELECT DATE_FORMAT(borrow_time, '%Y-%m-%d') AS date, COUNT(*) AS `value`
            FROM borrow_record
            WHERE borrow_time >= #{start}
            GROUP BY DATE_FORMAT(borrow_time, '%Y-%m-%d')
            ORDER BY date ASC
            """)
    List<TrendDTO> selectBorrowTrend(@Param("start") LocalDateTime start);

    @Select("""
            <script>
            SELECT br.*
            FROM borrow_record br
            LEFT JOIN book b ON b.id = br.book_id
            LEFT JOIN sys_user u ON u.id = br.user_id
            WHERE 1 = 1
            <if test="userId != null"> AND br.user_id = #{userId} </if>
            <if test="status != null and status != ''"> AND br.status = #{status} </if>
            <if test="keyword != null and keyword != ''">
                AND (b.title LIKE CONCAT('%', #{keyword}, '%') OR u.username LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            ORDER BY br.borrow_time DESC
            </script>
            """)
    IPage<BorrowRecord> selectBorrowPage(Page<BorrowRecord> page,
                                         @Param("userId") Long userId,
                                         @Param("status") String status,
                                         @Param("keyword") String keyword);
}
