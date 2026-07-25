package com.stonebridge.quotesystem.business.entity;
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
    // MAKING：制作中；SHIPPED：已寄出且已完成；ENDED：仅兼容历史完成数据
    private String status;
    private String trackingNo;
    private String remarks;
    private String creator;
    private LocalDateTime createTime;
    private LocalDateTime endTime;
    // ★ 新增：更新时间字段
    private LocalDateTime updateTime;
    @TableLogic
    private Integer delFlag;
    // 非数据库字段，用于前端特殊排序和展示
    @TableField(exist = false)
    private Integer sortGroup;
    @TableField(exist = false)
    private Boolean isOverdue;
}
