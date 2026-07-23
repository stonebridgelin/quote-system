package com.stonebridge.quotesystem.business.entity.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订单追踪列表展示 VO。
 *
 * 设计目的：
 * 1. 列表页不再直接展示 t_order_tracking 里的状态码；
 * 2. 后端在返回列表数据时，统一完成类型/状态翻译；
 * 3. 前端只展示 displayText/tagType，避免出现 2300、3300 这类裸状态码。
 */
@Data
public class OrderTrackingListVO {

    private String id;
    private String orderNo;
    private String customerOrderNo;
    private String customerContact;
    private String shapeDescription;
    private String reviewFormImageUrl;
    private String reviewFormImageKey;

    private Integer bottomLabel;
    private Integer bottomLabelStatus;
    private Integer isBottomLabelWaiting;
    private String bottomLabelTypeText;
    private String bottomLabelStatusText;
    private String bottomLabelDisplayText;
    private String bottomLabelTagType;

    private Integer sticker;
    private Integer stickerStatus;
    private Integer isStickerWaiting;
    private String stickerTypeText;
    private String stickerStatusText;
    private String stickerDisplayText;
    private String stickerTagType;

    private Integer printing;
    private Integer printingStatus;
    private String printingPatternCode;
    private Integer isPrintingWaiting;
    private String printingTypeText;
    private String printingStatusText;
    private String printingDisplayText;
    private String printingTagType;

    private Integer innerBox;
    private Integer innerBoxStatus;
    private Integer isInnerBoxWaiting;
    private String innerBoxTypeText;
    private String innerBoxStatusText;
    private String innerBoxDisplayText;
    private String innerBoxTagType;

    private Integer colorBox;
    private Integer colorBoxStatus;
    private Integer isColorBoxWaiting;
    private String colorBoxTypeText;
    private String colorBoxStatusText;
    private String colorBoxDisplayText;
    private String colorBoxTagType;

    private Integer cartonStatus;
    private Integer isCartonWaiting;
    private String cartonStatusText;
    private String cartonDisplayText;
    private String cartonTagType;

    private String separatorType;
    private Integer antiCutBoard;
    private String antiCutBoardText;
    private String antiCutBoardTagType;
    private String packagingRequirement;
    private LocalDate plannedProductionDate;
    private LocalDate plannedDeliveryDate;
    private Integer returnSample;
    private String remark;
    private Integer isFinished;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private String createBy;
    private LocalDateTime updateTime;
    private String updateBy;
}
