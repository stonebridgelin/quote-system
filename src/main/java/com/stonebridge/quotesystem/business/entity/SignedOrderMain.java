package com.stonebridge.quotesystem.business.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_signed_order_main")
public class SignedOrderMain {

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
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
}
