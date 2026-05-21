package com.stonebridge.quotesystem.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stonebridge.quotesystem.common.Result;
import com.stonebridge.quotesystem.entity.QuoteDetail;
import com.stonebridge.quotesystem.entity.dto.QuoteSaveDTO;
import com.stonebridge.quotesystem.service.IQuoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@RequestMapping("/api/quote")
@CrossOrigin
public class QuoteController {

    private IQuoteService quoteService;

    @Autowired
    public void setQuoteService(IQuoteService quoteService) {
        this.quoteService = quoteService;
    }

    /**
     * 核心：保存报价单
     */
    @PostMapping("/save")
    public Result<String> saveQuote(@RequestBody QuoteSaveDTO dto) {
        if (dto.getDetailList() == null || dto.getDetailList().isEmpty()) {
            return Result.fail("报价单明细不能为空");
        }
        if (dto.getExchangeRate() == null) {
            return Result.fail("汇率不能为空");
        }

        // 调用 Service 逻辑
        String quoteNo = quoteService.saveQuote(dto);
        return Result.success("保存成功", quoteNo);
    }

    /**
     * 导出报价单到 Excel
     * 注意：这里不能返回 Result 包装类，因为直接输出的是文件流
     */
    @GetMapping("/export/{quoteNo}")
    public void exportQuote(@PathVariable String quoteNo, HttpServletResponse response) {
        quoteService.exportQuote(quoteNo, response);
    }

    /**
     * 分页查询历史报价明细
     */
    @GetMapping("/history/page")
    public Result<Page<QuoteDetail>> getHistoryPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "15") Integer size,
            @RequestParam(required = false) String quoteNo,
            @RequestParam(required = false) String remarks) {
        return Result.success(quoteService.getHistoryPage(current, size, quoteNo, remarks));
    }

    /**
     * 根据单号获取该单号下的所有明细数据（不分页）
     */
    @GetMapping("/history/detail")
    public Result<List<QuoteDetail>> getDetailsByQuoteNo(@RequestParam String quoteNo) {
        return Result.success(quoteService.getDetailsByQuoteNo(quoteNo));
    }
}