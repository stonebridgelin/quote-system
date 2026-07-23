package com.stonebridge.quotesystem.business.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_customer")
public class Customer {

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    private String customerName;

    private LocalDateTime createTime;
}
