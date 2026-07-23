package com.stonebridge.quotesystem.business.entity.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SignedOrderDetailSaveDTO {

    @NotBlank(message = "商品明细类型不能为空")
    private String lineType;

    @Size(max = 100, message = "商品代号不能超过100个字符")
    private String productCode;

    private BigDecimal weight;

    /**
     * SINGLE 行由前端填写；SET 行由后端根据 totalSets 和组件数量重新计算。
     */
    private Long totalPcs;

    /**
     * SET 行的订购套数；SINGLE 行固定保存0。
     */
    private Long totalSets;

    @NotNull(message = "商品单价不能为空")
    @DecimalMin(value = "0", message = "商品单价不能小于0")
    private BigDecimal unitPrice;

    @Valid
    private List<SignedOrderSetItemSaveDTO> setItems;
}
