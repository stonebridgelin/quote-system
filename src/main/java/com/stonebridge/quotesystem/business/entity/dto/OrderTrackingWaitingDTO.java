package com.stonebridge.quotesystem.business.entity.dto;

import lombok.Data;

import java.util.List;

@Data
public class OrderTrackingWaitingDTO {

    /**
     * 订单ID
     */
    private String orderId;

    /**
     * 模块类型：
     * STICKER、PRINTING、INNER_BOX、COLOR_BOX、CARTON
     */
    private String moduleType;

    /**
     * 是否等待回复：0否，1是
     */
    private Integer waiting;

    /**
     * 当前模块类型值。
     * 底标、不干胶、花纸、黄盒、彩盒使用；外箱不使用。
     */
    private Integer typeValue;

    /**
     * 当前模块状态值。
     */
    private Integer statusValue;

    /**
     * 当前模块文本值。
     * 目前主要用于花纸工厂提供时的花型代码。
     */
    private String textValue;

    /**
     * 本次备注。
     * 用户可以在备注里写：问了客户什么、为什么等待。
     */
    private String remarkContent;

    /**
     * 本次等待操作对应的日志图片。
     */
    private List<OrderTrackingLogImageDTO> images;
}
