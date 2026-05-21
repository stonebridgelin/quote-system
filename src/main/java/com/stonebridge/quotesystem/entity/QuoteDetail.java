package com.stonebridge.quotesystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_quote_detail")
public class QuoteDetail {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String quoteNo;
    private Integer itemIndex;
    private String specCode;
    private String description;
    private String design;
    private Integer pcsPerSet;
    private Integer setsPerCtn;
    private Integer pcs;
    /**
     * 总件数 (PCS/SET * SETS/CTN * CTNs)
     */
    private Integer ttlPcs;
    private Integer ctns; // 可以为空
    private BigDecimal cbmCtn;
    private BigDecimal gwCtn;
    private BigDecimal nwCtn;
    private BigDecimal unitPrice;
    private BigDecimal weight;
    private String dimension;
    private BigDecimal originalPrice;
    private LocalDateTime createTime;
    @TableField("carton_weight")
    private BigDecimal cartonWeight;

    /**
     * 额外价格 (人民币：用于描边、贴花等)
     */
    private BigDecimal extraPrice;

    /**
     * 备注信息 (记录客户信息、计算逻辑等)
     */
    private String remarks;

    // --- 新增下面这两个虚拟字段 ---
    @TableField(exist = false)
    private BigDecimal price;  // 用于回显给前端的吨价

    @TableField(exist = false)
    private BigDecimal amount; // 用于在历史列表中展示的计算总金额
}