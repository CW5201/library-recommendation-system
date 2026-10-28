package com.library.recommendation.vo;

import lombok.Data;

import java.util.List;

@Data
public class ReadingPlanVO {

    private String content;
    private List<BookVO> books;
    private boolean aiGenerated;
}
