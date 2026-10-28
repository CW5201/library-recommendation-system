package com.library.recommendation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("book")
public class Book {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String author;
    private String publisher;
    private String isbn;
    private Long categoryId;
    private String description;
    private String cover;
    private LocalDate publishDate;
    private Integer totalStock;
    private Integer availableStock;
    private BigDecimal ratingAvg;
    private Integer ratingCount;
    private Integer borrowCount;
    private Integer viewCount;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
