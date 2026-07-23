package com.stonebridge.quotesystem.business.entity.dto;

import com.stonebridge.quotesystem.business.entity.QuoteDetail;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class QuoteSaveDTO {
    private String quoteNo;
    private String currency;
    private BigDecimal exchangeRate;

    // ★ 新增：接收前端弹窗输入的全局备注/客户标识
    private String remark;

    private List<QuoteDetail> detailList;
}