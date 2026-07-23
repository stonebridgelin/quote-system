package com.stonebridge.quotesystem.business.entity.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderTrackingLogVO {

    private String id;

    private String orderId;

    private String moduleType;

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

    private Long imageCount;

    private List<OrderTrackingLogImageVO> images;
}
