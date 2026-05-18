package com.stonebridge.quotesystem.entity;

import com.baomidou.mybatisplus.annotation.*;
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
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}