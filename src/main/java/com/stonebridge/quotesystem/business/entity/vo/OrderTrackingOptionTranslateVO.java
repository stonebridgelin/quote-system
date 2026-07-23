package com.stonebridge.quotesystem.business.entity.vo;

import lombok.Data;

/**
 * 订单追踪动态翻译返回 VO。
 */
@Data
public class OrderTrackingOptionTranslateVO {

    private String moduleType;
    private String optionType;
    private Integer parentValue;
    private Integer value;

    /**
     * 翻译后的显示名称。
     */
    private String label;

    /**
     * Element Plus el-tag 的 type：info、warning、success、danger、primary。
     */
    private String tagType;

    /**
     * 是否匹配到配置。
     */
    private Boolean matched;

    /**
     * 匹配方式：TYPE、PARENT、ALL、NONE、EMPTY。
     */
    private String matchedBy;

    /**
     * 对 STATUS 而言，是否严格匹配传入 parentValue。
     */
    private Boolean validForParent;
}
