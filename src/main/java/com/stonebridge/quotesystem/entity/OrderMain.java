package com.stonebridge.quotesystem.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_order_main")
public class OrderMain {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String orderNo;
    private String poNo;
    private String contact;
    private java.math.BigDecimal containerQty;
    private String containerType;
    private Integer totalQty;
    private Integer totalSets;
    private Integer totalCtns;
    private LocalDate deliveryDate;
    private Integer decalStatus;
    private Integer logoStatus;
    private Integer packageStatus;
    private LocalDate inspectionDate;
    private String preProdSample;
    private String signedSample;
    private String testSample;
    private String bulkSample;
    private String remark;
    private Integer status; // 0新建 1生产中 2完成
    private String creator;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @com.baomidou.mybatisplus.annotation.TableLogic
    private Integer delFlag;
}