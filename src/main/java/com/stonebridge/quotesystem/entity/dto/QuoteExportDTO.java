package com.stonebridge.quotesystem.entity.dto;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;
import java.math.BigDecimal;

@Data
@ContentRowHeight(80)
@HeadRowHeight(25)
public class QuoteExportDTO {

    @ExcelIgnore
    private String shapeCode;

    @ExcelIgnore
    private byte[] photo;

    // ★ 新增：用于在导出时记录分组下标，以便控制斑马纹单元格背景色
    @ExcelIgnore
    private Integer groupIndex;

    @ExcelProperty(value = "NO.", index = 0)
    private Integer itemIndex;

    @ExcelProperty(value = "ITEM NO.", index = 1)
    private String specCode;

    @ExcelProperty(value = "DESCRIPTION", index = 2)
    @ColumnWidth(30)
    private String description;

    @ExcelProperty(value = "Product photo", index = 3)
    @ColumnWidth(42)
    private String photoPlaceholder;

    @ExcelProperty(value = "DESIGN", index = 4)
    private String design;

    @ExcelProperty(value = "PCS/SET", index = 5)
    private Integer pcsPerSet;

    @ExcelProperty(value = "SETS/CTN", index = 6)
    private Integer setsPerCtn;

    @ExcelProperty(value = "PCS", index = 7)
    private Integer pcs;

    @ExcelProperty(value = "TTL PCS", index = 8)
    private Integer ttlPcs;

    @ExcelProperty(value = "TTL CTNs", index = 9)
    private Integer ctns;

    @ExcelProperty(value = {"CBM", "ctn"}, index = 10)
    private BigDecimal cbmCtn;

    @ExcelProperty(value = {"CBM", "total"}, index = 11)
    private BigDecimal cbmTotal;

    @ExcelProperty(value = {"N.W.(kg)", "ctn"}, index = 12)
    private BigDecimal nwCtn;

    @ExcelProperty(value = {"N.W.(kg)", "total"}, index = 13)
    private BigDecimal nwTotal;

    @ExcelProperty(value = {"G.W.(kg)", "ctn"}, index = 14)
    private BigDecimal gwCtn;

    @ExcelProperty(value = {"G.W.(kg)", "total"}, index = 15)
    private BigDecimal gwTotal;

    @ExcelProperty(value = "U.PRICE", index = 16)
    private BigDecimal unitPrice;

    @ExcelProperty(value = "AMOUNT", index = 17)
    @ColumnWidth(20)
    private BigDecimal amount;
}