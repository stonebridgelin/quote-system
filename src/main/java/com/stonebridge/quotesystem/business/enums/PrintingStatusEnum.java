package com.stonebridge.quotesystem.business.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PrintingStatusEnum implements TrackStatusEnum {

    CUSTOMER_CONFIRM(4100, "确认客户客供花纸"),
    CUSTOMER_SENT(4200, "客户已寄出"),
    RECEIVED_IN_WAREHOUSE(4300, "已收货入库"),
    WAREHOUSE_CONFIRMED(4400, "已和仓管确认无误");

    private final Integer code;
    private final String label;
}