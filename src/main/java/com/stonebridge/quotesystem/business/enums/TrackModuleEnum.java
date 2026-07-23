package com.stonebridge.quotesystem.business.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TrackModuleEnum {

    ORDER("ORDER", "整单日志"),
    BOTTOM_LABEL("BOTTOM_LABEL", "底标"),
    STICKER("STICKER", "不干胶"),
    PRINTING("PRINTING", "花纸"),
    INNER_BOX("INNER_BOX", "黄盒"),
    COLOR_BOX("COLOR_BOX", "彩盒"),
    CARTON("CARTON", "外箱");

    private final String code;
    private final String label;

    public static TrackModuleEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (TrackModuleEnum item : values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return null;
    }
}