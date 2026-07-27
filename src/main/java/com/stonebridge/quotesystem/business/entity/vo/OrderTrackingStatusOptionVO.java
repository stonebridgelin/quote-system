package com.stonebridge.quotesystem.business.entity.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderTrackingStatusOptionVO {

    private String id;

    private String moduleType;

    private String moduleLabel;

    private Integer parentValue;

    private String parentLabel;

    private Integer optionValue;

    private String optionLabel;

    private Integer sortNo;

    private String tagType;

    private String remark;

    private LocalDateTime updateTime;

    private String updateBy;
}
