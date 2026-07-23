// SampleSaveDTO.java
package com.stonebridge.quotesystem.business.entity.dto;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class SampleSaveDTO {
    private Long id; // 修改时有值
    private String customerInfo;
    private LocalDate planDate;
    private String remarks;
    private String status;
    private String trackingNo;
    private List<String> images; // 前端传来的 Base64 图片数组
}