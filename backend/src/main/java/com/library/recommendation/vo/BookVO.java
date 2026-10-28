package com.library.recommendation.vo;

import com.library.recommendation.entity.Book;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class BookVO {

    private Long id;
    private String title;
    private String author;
    private String publisher;
    private String isbn;
    private Long categoryId;
    private String categoryName;
    private String description;
    private String cover;
    private LocalDate publishDate;
    private Integer totalStock;
    private Integer availableStock;
    private BigDecimal ratingAvg;
    private Integer ratingCount;
    private Integer borrowCount;
    private Integer viewCount;
    private Integer status;
    private LocalDateTime createTime;

    private Boolean favorited;
    private Integer myScore;
    private Boolean borrowed;
    private Double similarity;
    private Double recommendationScore;
    private String reason;
    private String algorithm;

    public static BookVO from(Book book) {
        BookVO vo = new BookVO();
        BeanUtils.copyProperties(book, vo);
        vo.setFavorited(false);
        vo.setBorrowed(false);
        return vo;
    }
}
