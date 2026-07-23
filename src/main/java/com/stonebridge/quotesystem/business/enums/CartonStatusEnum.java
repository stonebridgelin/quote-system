package com.stonebridge.quotesystem.business.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CartonStatusEnum implements TrackStatusEnum {

    DESIGN_NOT_PROVIDED(1100, "客户尚未提供唛头 / 设计资料"),
    DESIGNING(1200, "客户提供了设计资料，公司正在设计"),
    CUSTOMER_APPROVED(1300, "用户已确认"),
    SALESMAN_SIGNED(1400, "业务员已签字确认"),
    CHECKED_OK(1500, "检查无误");

    private final Integer code;
    private final String label;
}