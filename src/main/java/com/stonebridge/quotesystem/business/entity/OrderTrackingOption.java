package com.stonebridge.quotesystem.business.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_order_tracking_option")
public class OrderTrackingOption {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String moduleType;

    private String optionType;

    private Integer parentValue;

    private Integer optionValue;

    private String optionLabel;

    private Integer sortNo;

    private String tagType;

    private String remark;

    private Integer isEnabled;

    private Integer isDeleted;

    private LocalDateTime createTime;

    private String createBy;

    private LocalDateTime updateTime;

    private String updateBy;
}