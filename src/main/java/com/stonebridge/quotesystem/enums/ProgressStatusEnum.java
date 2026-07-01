package com.stonebridge.quotesystem.enums;

import lombok.Getter;

@Getter
public enum ProgressStatusEnum {
    NONE(0, "无"),
    PENDING(1, "待确认"),
    CONFIRMED(2, "已确认");

    private final int code;
    private final String desc;

    ProgressStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
