package com.stonebridge.quotesystem.business.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TrackTypeEnum {

    NONE(10, "无 / 不需要"),
    CUSTOMER_PROVIDE(20, "客户提供"),
    FACTORY_PROVIDE(30, "工厂提供");

    private final Integer code;
    private final String label;

    public static String getLabel(Integer code) {
        if (code == null) {
            return "";
        }
        for (TrackTypeEnum item : values()) {
            if (item.code.equals(code)) {
                return item.label;
            }
        }
        return "";
    }
}