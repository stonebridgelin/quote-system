package com.stonebridge.quotesystem.business.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 套装报价组件快照。组件只记录构成和计价快照，套装公共属性仍保存在 QuoteDetail 父记录。
 */
@Data
@TableName("t_quote_set_item")
public class QuoteSetItem {

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;
    private String quoteDetailId;
    private Integer sortNo;
    private String specCode;

    @TableField(exist = false)
    private String description;

    private String design;
    private BigDecimal weight;
    private BigDecimal originalPrice;
    private Integer qtyPerSet;
    private BigDecimal componentUnitPrice;
    private LocalDateTime createTime;

    /** 兼容前端产品吨价字段，数据库统一保存为 original_price。 */
    @TableField(exist = false)
    private BigDecimal price;
}
