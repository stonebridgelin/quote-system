package com.stonebridge.quotesystem.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("t_quote_detail")
public class QuoteDetail {
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /** 兼容运行库原有的主外键列，不暴露给前端。 */
    @JsonIgnore
    @TableField("order_id")
    private String orderId;

    private String quoteNo;
    private Integer itemIndex;

    /** 前端使用的行排序字段，实际持久化到 item_index。 */
    @TableField(exist = false)
    private Integer sortNo;

    /** SINGLE：单品；SET：套装父记录。 */
    private String lineType;
    private String setGroupId;
    private String setName;
    private BigDecimal setUnitPrice;

    private String specCode;
    // ★ 修改：声明该字段不存于 t_quote_detail 数据库表中
    @TableField(exist = false)
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

    @TableField(exist = false)
    private String currency;

    /** 套装组件只保存在 t_quote_set_item，不写入报价明细父表。 */
    @TableField(exist = false)
    private List<QuoteSetItem> setItems;

    @TableField(exist = false)
    private BigDecimal cbmTotal;

    @TableField(exist = false)
    private BigDecimal nwTotal;

    @TableField(exist = false)
    private BigDecimal gwTotal;
}
