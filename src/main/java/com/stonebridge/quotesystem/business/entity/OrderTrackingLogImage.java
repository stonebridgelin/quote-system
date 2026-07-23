package com.stonebridge.quotesystem.business.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_order_tracking_log_image")
public class OrderTrackingLogImage {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String logId;

    private String orderId;

    private String moduleType;

    private String imageUrl;

    private String imageKey;

    private String originalName;

    private Long fileSize;

    private String contentType;

    /**
     * 图片排序号：同一日志内递增。
     */
    private Integer sortNo;

    private LocalDateTime createTime;

    private String createBy;
}
