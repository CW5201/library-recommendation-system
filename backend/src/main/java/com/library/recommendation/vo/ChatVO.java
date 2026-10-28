package com.library.recommendation.vo;

import lombok.Data;

import java.util.List;

@Data
public class ChatVO {

    private String sessionId;
    private String answer;
    private List<BookVO> relatedBooks;
    private boolean aiConfigured;
    private boolean fallback;
    private String notice;
}
