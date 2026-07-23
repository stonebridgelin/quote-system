package com.stonebridge.quotesystem.business.entity.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PackOptionVO {
    private Long id;
    private String packModel;
    private Integer pcsPerBox;
    private Integer boxesPerCarton;
    private BigDecimal outerLength;
    private BigDecimal outerWidth;
    private BigDecimal outerHeight;

    // 以下为后端根据前端传入的 weight 动态计算得出的字段
    private BigDecimal gwCtn; // 单箱毛重
    private BigDecimal cbmCtn; // 单箱体积 (顺手算好，减轻前端压力)
}