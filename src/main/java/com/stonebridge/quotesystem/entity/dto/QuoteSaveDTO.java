package com.stonebridge.quotesystem.entity.dto;

import com.stonebridge.quotesystem.entity.QuoteDetail;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class QuoteSaveDTO {
    // 【新增】：用于接收当前单号。为空说明是新建，有值说明是更新
    private String quoteNo;
    // 抬头信息
    private String currency;
    private BigDecimal exchangeRate;
    private String remark;

    // 明细列表
    private List<QuoteDetail> detailList;
}