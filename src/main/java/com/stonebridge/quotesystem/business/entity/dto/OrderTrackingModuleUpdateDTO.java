package com.stonebridge.quotesystem.business.entity.dto;

import lombok.Data;

import java.util.List;

@Data
public class OrderTrackingModuleUpdateDTO {

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
     * 当前类型：
     * 不干胶、花纸、黄盒、彩盒使用
     * 外箱不用
     */
    private Integer typeValue;

    /**
     * 当前状态
     */
    private Integer statusValue;

    /**
     * 文本字段：
     * 目前主要用于花纸工厂提供时的花型代码
     */
    private String textValue;

    /**
     * 本次新增日志备注
     */
    private String remarkContent;

    /**
     * 本次新增日志图片
     */
    private List<OrderTrackingLogImageDTO> images;
}