package com.library.recommendation.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ChatMessageVO {

    private Long id;
    private String role;
    private String content;
    private List<BookVO> relatedBooks;
    private LocalDateTime createTime;
}
