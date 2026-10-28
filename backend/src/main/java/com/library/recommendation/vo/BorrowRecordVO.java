package com.library.recommendation.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BorrowRecordVO {

    private Long id;
    private Long userId;
    private String username;
    private String nickname;
    private Long bookId;
    private String bookTitle;
    private String bookAuthor;
    private String cover;
    private LocalDateTime borrowTime;
    private LocalDateTime dueTime;
    private LocalDateTime returnTime;
    private String status;
    private Boolean overdue;
}
