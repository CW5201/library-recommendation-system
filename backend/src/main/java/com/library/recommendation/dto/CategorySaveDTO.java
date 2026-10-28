package com.library.recommendation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategorySaveDTO {

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称过长")
    private String name;

    @Size(max = 255, message = "分类介绍过长")
    private String description;

    private Integer sortOrder = 0;
}
