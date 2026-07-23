package com.stonebridge.quotesystem.business.entity.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SignedOrderDetailVO {

    private String id;
    private String orderNo;
    private String customerId;
    private String customerName;
    private String salesmanId;
    private String salesmanName;
    private LocalDate createDate;
    private String currency;
    private Long totalPcs;
    private Long totalSets;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime cancelTime;
    private String cancelReason;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String creator;
    private List<SignedOrderLineVO> detailList;
}
