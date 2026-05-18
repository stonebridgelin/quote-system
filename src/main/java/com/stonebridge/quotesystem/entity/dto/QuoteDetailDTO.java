package com.stonebridge.quotesystem.entity.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class QuoteDetailDTO {
    private String itemNo; // 器型规格代号
    private String description;
    private String design;
    private Integer pcsPerSet;
    private Integer setsPerCtn;
    private Integer ctns; // 用户填写的箱数

    // 以下原始数据用于后端校验计算，防止基础数据被修改导致计算错误
    private BigDecimal weight;
    private BigDecimal outerLength;
    private BigDecimal outerWidth;
    private BigDecimal outerHeight;
    private Integer tonPrice;
}