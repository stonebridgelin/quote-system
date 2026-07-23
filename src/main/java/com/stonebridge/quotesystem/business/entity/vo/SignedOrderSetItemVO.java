package com.stonebridge.quotesystem.business.entity.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SignedOrderSetItemVO {

    private String id;
    private String orderDetailId;
    private Integer sortNo;
    private String productCode;
    private BigDecimal weight;
    private Integer qtyPerSet;
    private LocalDateTime createTime;
}
