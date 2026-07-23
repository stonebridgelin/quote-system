package com.stonebridge.quotesystem.business.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_order_tracking_log")
public class OrderTrackingLog {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String orderId;

    private String moduleType;

    /**
     * 日志排序号：同一订单、同一模块内递增。
     * 用于避免同一秒内多条日志仅按 create_time 排序时顺序不稳定。
     */
    private Long sortNo;

    private Integer beforeType;

    private Integer afterType;

    private Integer beforeStatus;

    private Integer afterStatus;

    private String beforeText;

    private String afterText;

    private String remarkContent;

    private LocalDateTime createTime;

    private String createBy;
}
