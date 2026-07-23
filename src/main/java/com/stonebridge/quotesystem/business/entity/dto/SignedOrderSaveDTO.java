package com.stonebridge.quotesystem.business.entity.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class SignedOrderSaveDTO {

    @NotBlank(message = "订单编号不能为空")
    @Size(max = 100, message = "订单编号不能超过100个字符")
    private String orderNo;

    @Size(max = 32, message = "客户ID不能超过32个字符")
    private String customerId;

    @NotBlank(message = "客户名称不能为空")
    @Size(max = 200, message = "客户名称不能超过200个字符")
    private String customerName;

    @NotBlank(message = "业务员不能为空")
    @Size(max = 32, message = "业务员ID不能超过32个字符")
    private String salesmanId;

    private LocalDate createDate;

    @NotBlank(message = "币种不能为空")
    private String currency;

    @Size(max = 500, message = "订单备注不能超过500个字符")
    private String remark;

    @Valid
    @NotEmpty(message = "订单商品不能为空")
    private List<SignedOrderDetailSaveDTO> detailList;
}
