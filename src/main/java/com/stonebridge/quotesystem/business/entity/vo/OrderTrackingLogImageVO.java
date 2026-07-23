package com.stonebridge.quotesystem.business.entity.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderTrackingLogImageVO {

    private String id;

    private String logId;

    private String orderId;

    private String moduleType;

    private String imageUrl;

    private String imageKey;

    private String originalName;

    private Long fileSize;

    private String contentType;

    private Integer sortNo;

    private LocalDateTime createTime;

    private String createBy;
}
