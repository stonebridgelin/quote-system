package com.stonebridge.quotesystem.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stonebridge.quotesystem.entity.QuoteDetail;
import com.stonebridge.quotesystem.entity.QuoteMain;
import com.stonebridge.quotesystem.entity.dto.QuoteSaveDTO;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

public interface IQuoteService {
    String saveQuote(QuoteSaveDTO dto);

    void exportQuote(String quoteNo, HttpServletResponse response);
    // IQuoteService.java 中新增：

    Page<QuoteMain> getHistoryPage(Integer current, Integer size, String quoteNo, String remarks);

    List<QuoteDetail> getDetailsByQuoteNo(String quoteNo);
}