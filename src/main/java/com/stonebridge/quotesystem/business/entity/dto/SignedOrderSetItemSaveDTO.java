package com.stonebridge.quotesystem.business.entity.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SignedOrderSetItemSaveDTO {

    @NotBlank(message = "套装组成商品代号不能为空")
    @Size(max = 100, message = "商品代号不能超过100个字符")
    private String productCode;

    @NotNull(message = "套装组成商品重量不能为空")
    @DecimalMin(value = "0.001", message = "商品重量必须大于0")
    private BigDecimal weight;

    @NotNull(message = "每套商品数量不能为空")
    @Min(value = 1, message = "每套商品数量必须大于0")
    private Integer qtyPerSet;
}
