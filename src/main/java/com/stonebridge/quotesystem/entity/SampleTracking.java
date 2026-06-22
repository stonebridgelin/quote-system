package com.stonebridge.quotesystem.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_sample_tracking")
public class SampleTracking {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String customerInfo;
    private LocalDate planDate;
    private String status; // MAKING, SHIPPED, ENDED
    private String trackingNo;
    private String remarks;
    private String creator;
    private LocalDateTime createTime;
    private LocalDateTime endTime;

    // 非数据库字段，用于前端特殊排序和展示
    @TableField(exist = false)
    private Integer sortGroup;
    @TableField(exist = false)
    private Boolean isOverdue;
}