package com.stonebridge.quotesystem.business.entity.dto;

import lombok.Data;

@Data
public class PackSpecInputDTO {
    private Long id;
    private String packModel; // 包装型号输入，如 "XP80-6-8外箱"
    private String outerSizeStr; // 外箱尺寸字符串，如 "54.6*28.2*15.1"
    private String innerSizeStr; // 黄盒尺寸字符串，如 "13.3*13.2*13.3"
}