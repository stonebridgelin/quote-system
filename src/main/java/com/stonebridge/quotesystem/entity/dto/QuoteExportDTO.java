package com.stonebridge.quotesystem.entity.dto;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;
import java.math.BigDecimal;

@Data
@ContentRowHeight(80)   // 单行行高约 80pt，实际合并组高度 = 行数 × 80pt，图片会等比缩放填充
@HeadRowHeight(25)
public class QuoteExportDTO {

    /** 仅供 ShapeImageMergeStrategy 分组判断使用，不导出 */
    @ExcelIgnore
    private String shapeCode;

    /**
     * photo 改为 @ExcelIgnore：不让 EasyExcel 用 ByteArrayImageConverter 渲染。
     * 图片完全由 ShapeImageMergeStrategy#afterSheetDispose 用 POI 原生 Drawing API 绘制，
     * 这样才能读到合并后的真实总行高，实现精准居中。
     *
     * 注意：每行 DTO 的 photo 字段都要保留数据（不要置 null），
     * 策略内部自己按 shapeCode 分组，只取每组第一行的图片绘制。
     */
    @ExcelIgnore
    private byte[] photo;

    @ExcelProperty(value = "NO.", index = 0)
    private Integer itemIndex;

    @ExcelProperty(value = "ITEM NO.", index = 1)
    private String specCode;

    @ExcelProperty(value = "DESCRIPTION", index = 2)
    @ColumnWidth(30)
    private String description;

    /**
     * 占位列：宽度调整为 42（约 300 像素），完美匹配固定的图片尺寸
     */
    @ExcelProperty(value = "Product photo", index = 3)
    @ColumnWidth(42) // 修改这里，从 18 改为 42
    private String photoPlaceholder;

    @ExcelProperty(value = "DESIGN", index = 4)
    private String design;

    @ExcelProperty(value = "PCS/SET", index = 5)
    private Integer pcsPerSet;

    @ExcelProperty(value = "SETS/CTN", index = 6)
    private Integer setsPerCtn;

    @ExcelProperty(value = "PCS", index = 7)
    private Integer pcs;

    // 2. 新增 TTL PCS 字段及表头注解
    @ExcelProperty(value = "TTL PCS", index = 8)
    private Integer ttlPcs;

    // 1. 修改原有注解名称为 TTL CTNs
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
    private String unitPrice;

    @ExcelProperty(value = "AMOUNT", index = 17)
    @ColumnWidth(20)
    private String amount;
}