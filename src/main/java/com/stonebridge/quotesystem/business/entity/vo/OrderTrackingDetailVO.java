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
     * 仅返回整单日志最近几条摘要。
     *
     * key:
     * ORDER
     */
    private Map<String, List<OrderTrackingLogVO>> recentLogs;
}
