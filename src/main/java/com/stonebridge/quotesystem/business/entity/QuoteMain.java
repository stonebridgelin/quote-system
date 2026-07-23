package com.stonebridge.quotesystem.business.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_quote_main")
public class QuoteMain {
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 运行库保留的旧订单号列。新报价写入时与 quoteNo 保持一致，且不改变对外 JSON。
     */
    @JsonIgnore
    @TableField("order_no")
    private String orderNo;
    private String quoteNo;
    private String currency;
    private BigDecimal exchangeRate;
    // ★ 新增：映射数据库 t_quote_main 表的 remark 字段
    private String remark;
    private String creator;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
