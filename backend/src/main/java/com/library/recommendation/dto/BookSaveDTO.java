package com.library.recommendation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class BookSaveDTO {

    @NotBlank(message = "书名不能为空")
    @Size(max = 200, message = "书名过长")
    private String title;

    @NotBlank(message = "作者不能为空")
    @Size(max = 120, message = "作者名过长")
    private String author;

    @Size(max = 120, message = "出版社名称过长")
    private String publisher;

    @NotBlank(message = "ISBN 不能为空")
    @Size(max = 20, message = "ISBN 过长")
    private String isbn;

    @NotNull(message = "请选择分类")
    private Long categoryId;

    @Size(max = 5000, message = "简介过长")
    private String description;

    @Size(max = 255, message = "封面地址过长")
    private String cover;

    private LocalDate publishDate;

    @NotNull(message = "请填写馆藏总量")
    @Min(value = 0, message = "馆藏总量不能小于 0")
    @Max(value = 99999, message = "馆藏总量过大")
    private Integer totalStock;

    @NotNull(message = "请填写可借数量")
    @Min(value = 0, message = "可借数量不能小于 0")
    @Max(value = 99999, message = "可借数量过大")
    private Integer availableStock;

    private Integer status = 1;
}
