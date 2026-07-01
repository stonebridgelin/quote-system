// OrderRecord.java (跟进记录主表)
package com.stonebridge.quotesystem.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_order_record")
public class OrderRecord {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String orderId;
    private Integer itemIndex;
    private String remark;
    private String creator;
    private LocalDateTime createTime;
}