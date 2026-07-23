package com.stonebridge.quotesystem.business.entity.dto;

import lombok.Data;

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
     * 本次备注。
     * 用户可以在备注里写：问了客户什么、为什么等待。
     */
    private String remarkContent;
}
