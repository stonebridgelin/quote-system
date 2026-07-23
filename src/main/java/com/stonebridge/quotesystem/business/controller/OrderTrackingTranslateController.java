package com.stonebridge.quotesystem.business.controller;

import com.stonebridge.quotesystem.common.Result;
import com.stonebridge.quotesystem.business.entity.dto.OrderTrackingOptionTranslateDTO;
import com.stonebridge.quotesystem.business.entity.vo.OrderTrackingOptionTranslateVO;
import com.stonebridge.quotesystem.business.service.IOrderTrackingOptionTranslateService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 订单追踪动态翻译接口。
 *
 * 使用场景：
 * 1. 列表页不需要调用本接口，因为 /order-tracking/page 已经返回翻译后的 VO；
 * 2. 其他动态场景如果拿到的是原始码，可以通过本接口批量翻译；
 * 3. 配置项低频修改、高频读取，翻译服务内部有内存缓存。
 */
@RestController
@RequestMapping("/order-tracking/translate")
@RequiredArgsConstructor
public class OrderTrackingTranslateController {

    private final IOrderTrackingOptionTranslateService translateService;

    /**
     * 单个配置项翻译。
     * <p>
     * 只要拥有订单追踪查询或详情权限，就允许使用翻译能力。
     */
    @PreAuthorize("hasAnyAuthority('order-tracking:page', 'order-tracking:detail')")
    @GetMapping("/option")
    public Result<OrderTrackingOptionTranslateVO> translateOption(
            @RequestParam String moduleType,
            @RequestParam String optionType,
            @RequestParam(required = false) Integer parentValue,
            @RequestParam Integer value
    ) {
        return Result.success(translateService.translate(moduleType, optionType, parentValue, value));
    }

    /**
     * 批量配置项翻译。
     */
    @PreAuthorize("hasAnyAuthority('order-tracking:page', 'order-tracking:detail')")
    @PostMapping("/options")
    public Result<List<OrderTrackingOptionTranslateVO>> translateOptions(
            @RequestBody List<OrderTrackingOptionTranslateDTO> requests
    ) {
        return Result.success(translateService.translateBatch(requests));
    }

    /**
     * 刷新订单追踪配置翻译缓存。
     * <p>
     * 这是配置维护类操作，归入订单追踪更新权限。
     */
    @PreAuthorize("hasAuthority('order-tracking:update')")
    @PostMapping("/cache/refresh")
    public Result<Void> refreshCache() {
        translateService.refreshCache();
        return Result.success();
    }
}
