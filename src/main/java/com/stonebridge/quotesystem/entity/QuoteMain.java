package com.stonebridge.quotesystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_quote_main")
public class QuoteMain {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String quoteNo;
    private String currency;
    private BigDecimal exchangeRate;
    // ★ 新增：映射数据库 t_quote_main 表的 remark 字段
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}