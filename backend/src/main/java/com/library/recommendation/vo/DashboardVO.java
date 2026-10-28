package com.library.recommendation.vo;

import lombok.Data;

import java.util.List;

@Data
public class DashboardVO {

    private long userCount;
    private long bookCount;
    private long categoryCount;
    private long borrowCount;
    private long activeBorrowCount;
    private long todayBorrowCount;
    private long behaviorCount;
    private long aiChatCount;
    private List<BorrowRecordVO> recentBorrows;
    private List<BookVO> hotBooks;
}
