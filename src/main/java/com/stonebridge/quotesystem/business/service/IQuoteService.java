package com.stonebridge.quotesystem.business.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stonebridge.quotesystem.business.entity.QuoteDetail;
import com.stonebridge.quotesystem.business.entity.QuoteMain;
import com.stonebridge.quotesystem.business.entity.dto.QuoteSaveDTO;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

public interface IQuoteService {
    String saveQuote(QuoteSaveDTO dto);

    void exportQuote(String quoteNo, List<String> columns, String headerLang, HttpServletResponse response);    // IQuoteService.java 中新增：

    Page<QuoteMain> getHistoryPage(Integer current, Integer size, String quoteNo, String remarks);

    List<QuoteDetail> getDetailsByQuoteNo(String quoteNo);
}