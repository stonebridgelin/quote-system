package com.stonebridge.quotesystem.business.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CartonStatusEnum implements TrackStatusEnum {

    DESIGN_NOT_PROVIDED(1100, "客户尚未提供唛头 / 设计资料"),
    DESIGN_MATERIAL_RECEIVED(1200, "客户已提供设计资料"),
    DESIGN_SENT_WAITING_CONFIRMATION(1250, "设计初步完成，已发客户待确认"),
    CUSTOMER_APPROVED(1300, "用户已确认"),
    SALESMAN_SIGNED(1400, "业务员已签字确认");

    private final Integer code;
    private final String label;
}
