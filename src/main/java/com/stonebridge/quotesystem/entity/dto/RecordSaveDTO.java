// RecordSaveDTO.java (用于保存更新记录及图片)
package com.stonebridge.quotesystem.entity.dto;
import lombok.Data;
import java.util.List;

@Data
public class RecordSaveDTO {
    private String orderId;
    private String remark;
    private List<String> images; // 前端传来的 Base64 图片数组
}