package com.stonebridge.quotesystem.business.entity.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SignedOrderPageQueryDTO {

    @Min(value = 1, message = "页码不能小于1")
    private Integer current = 1;

    @Min(value = 1, message = "每页数量不能小于1")
    @Max(value = 200, message = "每页数量不能超过200")
    private Integer size = 15;

    @Size(max = 200, message = "客户名称不能超过200个字符")
    private String customerName;

    @Size(max = 100, message = "订单编号不能超过100个字符")
    private String orderNo;

    @Size(max = 32, message = "业务员ID不能超过32个字符")
    private String salesmanId;

    private LocalDate createDateStart;

    private LocalDate createDateEnd;

    @DecimalMin(value = "0", message = "最小订单金额不能小于0")
    private BigDecimal amountMin;

    @DecimalMin(value = "0", message = "最大订单金额不能小于0")
    private BigDecimal amountMax;
}
