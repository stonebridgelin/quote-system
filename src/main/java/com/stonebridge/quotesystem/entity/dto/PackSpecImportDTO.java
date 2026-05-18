package com.stonebridge.quotesystem.entity.dto;


import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class PackSpecImportDTO {
    @ExcelProperty("包装型号")
    private String packModel; // 如 "XP80-6-8外箱"

    @ExcelProperty("外箱尺寸(长*宽*高)")
    private String outerSizeStr; // 如 "54.6*28.2*15.1"

    @ExcelProperty("黄盒尺寸(长*宽*高)")
    private String innerSizeStr;
}