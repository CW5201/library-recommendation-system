package com.library.recommendation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("borrow_record")
public class BorrowRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long bookId;
    private LocalDateTime borrowTime;
    private LocalDateTime dueTime;
    private LocalDateTime returnTime;
    private String status;
    private LocalDateTime createTime;
}
