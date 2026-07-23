package com.stonebridge.quotesystem.business.entity.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class OrderTrackingSaveDTO {

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

    private Integer returnSample;

    private String remark;

    private Integer isFinished;

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

    /**
     * 本次各模块新增日志。
     * 新增订单时，如果某个模块填写了备注或图片，也可以生成初始日志。
     * 编辑订单时，用于保存每个模块本次备注和本次图片。
     */
    private List<OrderTrackingModuleUpdateDTO> moduleUpdates;
}