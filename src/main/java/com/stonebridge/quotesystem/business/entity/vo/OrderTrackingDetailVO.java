package com.stonebridge.quotesystem.business.entity.vo;

import com.stonebridge.quotesystem.business.entity.OrderTracking;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class OrderTrackingDetailVO {

    /**
     * 订单主信息
     */
    private OrderTracking order;

    /**
     * 主弹窗展示用：
     * 每个模块最近几条日志摘要。
     *
     * key:
     * STICKER、PRINTING、INNER_BOX、COLOR_BOX、CARTON
     */
    private Map<String, List<OrderTrackingLogVO>> recentLogs;
}