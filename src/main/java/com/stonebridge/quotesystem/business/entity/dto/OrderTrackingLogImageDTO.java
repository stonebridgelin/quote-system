package com.stonebridge.quotesystem.business.entity.dto;

import lombok.Data;

@Data
public class OrderTrackingLogImageDTO {

    private String imageUrl;

    private String imageKey;

    private String originalName;

    private Long fileSize;

    private String contentType;

    /**
     * 图片排序号：同一日志内递增。前端不传时，后端按上传顺序自动生成。
     */
    private Integer sortNo;
}
