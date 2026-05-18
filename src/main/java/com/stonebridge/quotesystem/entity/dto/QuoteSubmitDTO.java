package com.stonebridge.quotesystem.entity.dto;

import lombok.Data;
import java.util.List;

@Data
public class QuoteSubmitDTO {
    private String remark; // 报价备注
    private List<QuoteDetailDTO> detailList; // 报价明细列表
}