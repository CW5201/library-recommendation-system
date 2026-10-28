package com.library.recommendation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("book_embedding")
public class BookEmbedding {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long bookId;
    private String model;
    private Integer dimension;
    private String vector;
    private String contentHash;
    private LocalDateTime updateTime;
}
