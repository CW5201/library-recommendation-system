package com.library.recommendation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.recommendation.dto.InteractionDTO;
import com.library.recommendation.dto.NameValueDTO;
import com.library.recommendation.entity.UserBehavior;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserBehaviorMapper extends BaseMapper<UserBehavior> {

    @Select("""
            SELECT user_id AS userId, book_id AS bookId,
                   SUM(CASE behavior_type
                           WHEN 'BORROW' THEN 5
                           WHEN 'RATE' THEN 4
                           WHEN 'FAVORITE' THEN 3
                           WHEN 'VIEW' THEN 1
                           ELSE 0 END) AS weight
            FROM user_behavior
            GROUP BY user_id, book_id
            """)
    List<InteractionDTO> selectInteractions();

    @Select("""
            SELECT behavior_type AS name, COUNT(*) AS `value`
            FROM user_behavior
            GROUP BY behavior_type
            ORDER BY `value` DESC
            """)
    List<NameValueDTO> selectBehaviorStats();
}
