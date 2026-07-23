package com.stonebridge.quotesystem.business.controller;

import com.stonebridge.quotesystem.common.Result;
import com.stonebridge.quotesystem.business.entity.vo.OrderTrackingOptionVO;
import com.stonebridge.quotesystem.business.service.IOrderTrackingOptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class OrderTrackingOptionController {

    private final IOrderTrackingOptionService orderTrackingOptionService;

    /**
     * 订单追踪配置项。
     * <p>
     * 列表页、新增页、编辑页都会使用这些配置项渲染模块类型和状态，
     * 因此只要求具备订单追踪查询权限即可访问。
     */
    @PreAuthorize("hasAuthority('order-tracking:page')")
    @GetMapping("/order-tracking/options")
    public Result<Map<String, List<OrderTrackingOptionVO>>> options() {
        return Result.success(orderTrackingOptionService.getOptionMap());
    }
}
