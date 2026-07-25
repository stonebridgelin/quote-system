package com.stonebridge.quotesystem.business.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum InnerBoxStatusEnum implements TrackStatusEnum {

    DESIGN_NOT_PROVIDED(5100, "客户尚未提供设计资料"),
    DESIGN_MATERIAL_RECEIVED(5200, "客户已提供设计资料"),
    DESIGN_SENT_WAITING_CONFIRMATION(5250, "设计初步完成，已发客户待确认"),
    CUSTOMER_APPROVED(5300, "用户已确认"),
    SALESMAN_SIGNED(5400, "业务员已签字确认");

    private final Integer code;
    private final String label;
}
