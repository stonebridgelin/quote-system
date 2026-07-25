package com.stonebridge.quotesystem.business.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ColorBoxStatusEnum implements TrackStatusEnum {

    CUSTOMER_CONFIRM(6100, "确认客供彩盒"),
    CUSTOMER_SENT(6200, "客户已寄出"),
    RECEIVED_IN_WAREHOUSE(6300, "已收货入库"),
    WAREHOUSE_CONFIRMED(6400, "已和仓管确认无误"),

    DESIGN_NOT_PROVIDED(7100, "客户确认存在但未提供设计资料"),
    DESIGN_MATERIAL_RECEIVED(7200, "客户已提供设计资料"),
    DESIGN_SENT_WAITING_CONFIRMATION(7250, "设计初步完成，已发客户待确认"),
    CUSTOMER_APPROVED(7300, "用户已确认"),
    SALESMAN_SIGNED(7400, "业务员已签字确认");

    private final Integer code;
    private final String label;
}
