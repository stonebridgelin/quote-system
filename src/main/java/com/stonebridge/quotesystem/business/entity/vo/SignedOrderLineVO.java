package com.stonebridge.quotesystem.business.entity.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SignedOrderLineVO {

    private String id;
    private String orderId;
    private Integer sortNo;
    private String lineType;
    private String productCode;
    private BigDecimal weight;
    private Long totalPcs;
    private Long totalSets;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private LocalDateTime createTime;
    private List<SignedOrderSetItemVO> setItems;
}
