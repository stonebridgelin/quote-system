package com.stonebridge.quotesystem.business.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_signed_order_set_item")
public class SignedOrderSetItem {

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    private String orderDetailId;
    private Integer sortNo;
    private String productCode;
    private BigDecimal weight;
    private Integer qtyPerSet;
    private LocalDateTime createTime;
}
