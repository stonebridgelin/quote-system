package com.stonebridge.quotesystem.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_order_tracking")
public class OrderTracking {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String orderNo;

    private String customerOrderNo;

    private String customerContact;

    private String shapeDescription;

    private String reviewFormImageUrl;

    private String reviewFormImageKey;

    private Integer bottomLabel;

    private Integer bottomLabelStatus;

    @TableField("is_bottom_label_waiting")
    private Integer isBottomLabelWaiting;

    private Integer sticker;

    private Integer stickerStatus;

    private Integer printing;

    private Integer printingStatus;

    private String printingPatternCode;

    private Integer innerBox;

    private Integer innerBoxStatus;

    private Integer colorBox;

    private Integer colorBoxStatus;

    private Integer cartonStatus;

    private String separatorType;

    private Integer antiCutBoard;

    private String packagingRequirement;

    private LocalDate plannedProductionDate;

    private LocalDate plannedDeliveryDate;

    @TableField("is_sticker_waiting")
    private Integer isStickerWaiting;

    @TableField("is_printing_waiting")
    private Integer isPrintingWaiting;

    @TableField("is_inner_box_waiting")
    private Integer isInnerBoxWaiting;

    @TableField("is_color_box_waiting")
    private Integer isColorBoxWaiting;

    @TableField("is_carton_waiting")
    private Integer isCartonWaiting;

    private Integer returnSample;

    private String remark;

    private Integer isFinished;

    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;

    private LocalDateTime createTime;

    private String createBy;

    private LocalDateTime updateTime;

    private String updateBy;
}