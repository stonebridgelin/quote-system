// OrderDetail.java (订单明细)
package com.stonebridge.quotesystem.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("t_order_detail")
public class OrderDetail {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String orderId;
    private Integer itemIndex;
    private String customerItemNo;
    private String shape;
    private Integer pcsPerSet;
    private Integer setsPerCtn;
    private Integer qty;
    private Integer sets;
    private Integer ctns;
    private String decal;
    private String packageReq;
    private String comboMark;
}