package com.stonebridge.quotesystem.business.entity.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class ShapeSpecImportDTO {
    @ExcelProperty("器型代号")
    private String shapeCode;

    @ExcelProperty("器型规格代码")
    private String specCode;

    @ExcelProperty("重量(g)")
    private String weight; // 用 String 接收，容错率高，稍后转 BigDecimal

    @ExcelProperty("DESCRIPTION")
    private String description;

    @ExcelProperty("吨价(元/t)")
    private Integer tonPrice;
}