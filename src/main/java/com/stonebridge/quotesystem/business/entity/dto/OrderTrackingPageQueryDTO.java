package com.stonebridge.quotesystem.business.entity.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class OrderTrackingPageQueryDTO {

    private Integer current = 1;

    private Integer size = 10;

    /**
     * 同时模糊查询：
     * 订单编号、客户订单编号、客户对接人
     */
    private String keyword;

    private Integer bottomLabel;

    private Integer bottomLabelStatus;

    private Integer sticker;

    private Integer stickerStatus;

    private Integer printing;

    private Integer printingStatus;

    private Integer innerBox;

    private Integer innerBoxStatus;

    private Integer colorBox;

    private Integer colorBoxStatus;

    private Integer cartonStatus;

    private Integer antiCutBoard;

    private LocalDate plannedProductionDateStart;

    private LocalDate plannedProductionDateEnd;

    private LocalDate plannedDeliveryDateStart;

    private LocalDate plannedDeliveryDateEnd;

    private Integer isFinished;
}