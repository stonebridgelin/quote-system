package com.stonebridge.quotesystem.business.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum InnerBoxStatusEnum implements TrackStatusEnum {

    DESIGN_NOT_PROVIDED(5100, "客户尚未提供设计资料"),
    DESIGNING(5200, "客户提供了设计资料，公司正在设计"),
    CUSTOMER_APPROVED(5300, "用户已确认"),
    SALESMAN_SIGNED(5400, "业务员已签字确认"),
    CHECKED_OK(5500, "检查无误");

    private final Integer code;
    private final String label;
}