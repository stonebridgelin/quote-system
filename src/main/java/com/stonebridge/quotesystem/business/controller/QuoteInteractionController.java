package com.stonebridge.quotesystem.business.controller;

import com.stonebridge.quotesystem.business.entity.Shape;
import com.stonebridge.quotesystem.business.entity.ShapeSpec;
import com.stonebridge.quotesystem.business.service.IQuoteInteractionService;
import com.stonebridge.quotesystem.business.entity.vo.PackOptionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/interaction")
@CrossOrigin // 解决 Vue 前端跨域
public class QuoteInteractionController {

    private IQuoteInteractionService interactionService;

    @Autowired
    public void setInteractionService(IQuoteInteractionService interactionService) {
        this.interactionService = interactionService;
    }

    /**
     * Level 2 弹窗：搜索器型
     */
    @PreAuthorize("hasAnyAuthority('quote:create', 'quote:update', 'quote:page')")
    @GetMapping("/shape/search")
    public Map<String, Object> searchShape(@RequestParam(required = false) String keyword) {
        List<Shape> result = interactionService.searchShape(keyword);
        return success(result);
    }

    /**
     * Level 1 弹窗：获取规格列表（已按尺寸升序排列）
     */
    @PreAuthorize("hasAnyAuthority('quote:create', 'quote:update', 'quote:page')")
    @GetMapping("/shape-spec/list")
    public Map<String, Object> getShapeSpecs(@RequestParam String shapeCode) {
        List<ShapeSpec> result = interactionService.getShapeSpecsByShapeCode(shapeCode);
        return success(result);
    }

    /**
     * Level 3 弹窗：获取动态包装方案列表（带毛重计算，并按毛重降序排列）
     */
    @PreAuthorize("hasAnyAuthority('quote:create', 'quote:update', 'quote:page')")
    @GetMapping("/pack-spec/options")
    public Map<String, Object> getPackOptions(
            @RequestParam String specCode,
            @RequestParam BigDecimal weight) { // 必须前端传入该规格对应的重量

        List<PackOptionVO> result = interactionService.getPackOptions(specCode, weight);
        return success(result);
    }

    // 快捷响应封装
    private Map<String, Object> success(Object data) {
        Map<String, Object> res = new HashMap<>();
        res.put("code", 200);
        res.put("data", data);
        return res;
    }
}