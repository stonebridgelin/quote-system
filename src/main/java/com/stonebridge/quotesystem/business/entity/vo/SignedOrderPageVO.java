package com.stonebridge.quotesystem.business.entity.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SignedOrderPageVO {

    private Long current;
    private Long size;
    private Long total;
    private List<SignedOrderListVO> records;
    private BigDecimal cnyTotal;
    private BigDecimal usdTotal;
}
