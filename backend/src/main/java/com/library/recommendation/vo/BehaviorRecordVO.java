package com.library.recommendation.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BehaviorRecordVO {

    private Long id;
    private String behaviorType;
    private String remark;
    private LocalDateTime createTime;
    private BookVO book;
}
