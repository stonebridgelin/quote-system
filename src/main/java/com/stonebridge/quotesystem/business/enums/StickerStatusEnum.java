package com.stonebridge.quotesystem.business.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StickerStatusEnum implements TrackStatusEnum {

    CUSTOMER_CONFIRM(2100, "确认客供不干胶"),
    CUSTOMER_SENT(2200, "客户已寄出"),
    RECEIVED_IN_WAREHOUSE(2300, "已收货入库"),
    WAREHOUSE_CONFIRMED(2400, "已和仓管确认无误"),

    DESIGN_NOT_PROVIDED(3100, "客户确认存在但未提供设计资料"),
    DESIGNING(3200, "客户提供了设计资料，公司正在设计"),
    CUSTOMER_APPROVED(3300, "用户已确认"),
    SALESMAN_SIGNED(3400, "业务员已签字确认"),
    CHECKED_OK(3500, "检查无误");

    private final Integer code;
    private final String label;
}