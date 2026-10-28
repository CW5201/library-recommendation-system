package com.library.recommendation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("book_category")
public class BookCategory {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private Integer sortOrder;
    private LocalDateTime createTime;
}
