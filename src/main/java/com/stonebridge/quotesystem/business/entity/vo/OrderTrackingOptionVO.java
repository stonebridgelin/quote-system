package com.stonebridge.quotesystem.business.entity.vo;

import lombok.Data;

@Data
public class OrderTrackingOptionVO {

    private String moduleType;

    private String optionType;

    private Integer parentValue;

    private Integer value;

    private String label;

    private Integer sortNo;

    private String tagType;

    private String remark;
}