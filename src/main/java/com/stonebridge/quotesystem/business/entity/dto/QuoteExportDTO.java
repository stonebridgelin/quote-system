package com.stonebridge.quotesystem.business.entity.dto;

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

    // 不需要导出的字段保留 @ExcelIgnore
    @ExcelIgnore
    private String shapeCode;

    @ExcelIgnore
    private byte[] photo;

    @ExcelIgnore
    private Integer groupIndex;

    /** 同一报价父明细展开后的所有组件共享该键，用于套装公共单元格合并。 */
    @ExcelIgnore
    private String quoteLineKey;

    @ExcelIgnore
    private Boolean setLine;

    // ★ 下面所有的 @ExcelProperty 移除了固定的 index 属性，使其支持动态自然平移
    @ExcelProperty("NO.")
    private Integer itemIndex;

    @ExcelProperty("ITEM NO.")
    private String specCode;

    @ExcelProperty("DESCRIPTION")
    @ColumnWidth(30)
    private String description;

    @ExcelProperty("Product photo")
    @ColumnWidth(42)
    private String photoPlaceholder;

    @ExcelProperty("DESIGN")
    private String design;

    @ExcelProperty("PCS/SET")
    private Integer pcsPerSet;

    @ExcelProperty("SETS/CTN")
    private Integer setsPerCtn;

    @ExcelProperty("PCS")
    private Integer pcs;

    @ExcelProperty("TTL PCS")
    private Integer ttlPcs;

    @ExcelProperty("TTL CTNs")
    private Integer ctns;

    @ExcelProperty({"CBM", "ctn"})
    private BigDecimal cbmCtn;

    @ExcelProperty({"CBM", "total"})
    private BigDecimal cbmTotal;

    @ExcelProperty({"N.W.(kg)", "ctn"})
    private BigDecimal nwCtn;

    @ExcelProperty({"N.W.(kg)", "total"})
    private BigDecimal nwTotal;

    @ExcelProperty({"G.W.(kg)", "ctn"})
    private BigDecimal gwCtn;

    @ExcelProperty({"G.W.(kg)", "total"})
    private BigDecimal gwTotal;

    @ExcelProperty("U.PRICE")
    private BigDecimal unitPrice;

    @ExcelProperty("AMOUNT")
    @ColumnWidth(20)
    private BigDecimal amount;

    // --- 动态可选列 ---

    @ExcelProperty("Weight(g)")
    private BigDecimal weight;

    @ExcelProperty("Volume(cm)")
    @ColumnWidth(15)
    private String dimension;

    @ExcelProperty("Price(吨价)")
    private BigDecimal price;

    @ExcelProperty("额外价格(¥)")
    private BigDecimal extraPrice;

    @ExcelProperty("Carton(kg)")
    private BigDecimal cartonWeight;

    @ExcelProperty("备注信息")
    @ColumnWidth(25)
    private String remarks;
}
