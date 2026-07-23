package com.stonebridge.quotesystem.business.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SignedOrderCancelDTO {

    @NotBlank(message = "取消原因不能为空")
    @Size(max = 500, message = "取消原因不能超过500个字符")
    private String cancelReason;
}
