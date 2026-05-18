package com.stonebridge.quotesystem.entity.vo;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import com.alibaba.excel.converters.bytearray.ByteArrayImageConverter;
import lombok.Data;
import java.math.BigDecimal;

@Data
@ContentRowHeight(80)
@HeadRowHeight(25)
public class QuoteExportVO {

    // 【极其重要：找回丢失的字段】
    // 隐藏了 original_weight, original_volume, original_price，但这个 shapeCode 绝对不能省！
    // 它是我们自定义拦截器 ShapeImageMergeStrategy 用来判断“上下两行是不是同一器型”的唯一凭证
    @ExcelIgnore
    private String shapeCode;

    // --- 下面通过 index 属性，强行锁定每一列在 Excel 中的绝对位置 ---

    @ExcelProperty(value = "NO.", index = 0)
    private Integer itemIndex;

    @ExcelProperty(value = "ITEM NO.", index = 1)
    private String specCode;

    @ExcelProperty(value = "DESCRIPTION", index = 2)
    @ColumnWidth(30)
    private String description;

    // 【核心锁定】：index = 3 强制把它钉死在第 4 列！哪怕反射乱了它也不会跑到最后面。
    @ExcelProperty(value = "Product photo", converter = ByteArrayImageConverter.class, index = 3)
    @ColumnWidth(18)
    private byte[] photo;

    @ExcelProperty(value = "DESIGN", index = 4)
    private String design;

    @ExcelProperty(value = "PCS/SET", index = 5)
    private Integer pcsPerSet;

    @ExcelProperty(value = "SETS/CTN", index = 6)
    private Integer setsPerCtn;

    @ExcelProperty(value = "PCS", index = 7)
    private Integer pcs;

    @ExcelProperty(value = "CTNs", index = 8)
    private Integer ctns;

    @ExcelProperty(value = {"CBM", "ctn"}, index = 9) // 复杂表头
    private BigDecimal cbmCtn;

    @ExcelProperty(value = {"CBM", "total"}, index = 10)
    private BigDecimal cbmTotal;

    @ExcelProperty(value = {"G.W.(kg)", "ctn"}, index = 11)
    private BigDecimal gwCtn;

    @ExcelProperty(value = {"G.W.(kg)", "total"}, index = 12)
    private BigDecimal gwTotal;

    @ExcelProperty(value = "U.PRICE(USD)", index = 13)
    private BigDecimal unitPriceUsd;

    @ExcelProperty(value = "AMOUNT(USD)", index = 14)
    @ColumnWidth(20)
    private BigDecimal amountUsd;
}