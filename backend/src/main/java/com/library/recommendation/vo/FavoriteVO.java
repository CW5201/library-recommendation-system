package com.library.recommendation.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FavoriteVO {

    private BookVO book;
    private LocalDateTime createTime;
}
