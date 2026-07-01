// OrderRecordImage.java (跟进记录图片)
package com.stonebridge.quotesystem.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_order_record_image")
public class OrderRecordImage {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String recordId;
    private String base64Data;
    private LocalDateTime createTime;
}