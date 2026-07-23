package com.stonebridge.quotesystem.business.entity.dto;

import lombok.Data;

/**
 * 订单追踪动态翻译请求 DTO。
 */
@Data
public class OrderTrackingOptionTranslateDTO {

    /**
     * 模块类型：STICKER、PRINTING、INNER_BOX、COLOR_BOX、CARTON
     */
    private String moduleType;

    /**
     * 配置类型：TYPE、STATUS
     */
    private String optionType;

    /**
     * 状态所属父级类型值。TYPE 翻译时可不传；STATUS 翻译建议传。
     */
    private Integer parentValue;

    /**
     * 待翻译的原始值。
     */
    private Integer value;
}
