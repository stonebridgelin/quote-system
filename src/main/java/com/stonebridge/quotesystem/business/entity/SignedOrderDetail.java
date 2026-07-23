package com.stonebridge.quotesystem.business.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_signed_order_detail")
public class SignedOrderDetail {

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
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
}
