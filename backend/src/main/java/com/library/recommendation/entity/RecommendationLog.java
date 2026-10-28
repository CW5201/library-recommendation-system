package com.library.recommendation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("recommendation_log")
public class RecommendationLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long bookId;
    private String algorithm;
    private BigDecimal score;
    private String reason;
    private LocalDateTime createTime;
}
