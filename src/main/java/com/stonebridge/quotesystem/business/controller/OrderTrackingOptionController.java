package com.stonebridge.quotesystem.business.controller;

import com.stonebridge.quotesystem.common.Result;
import com.stonebridge.quotesystem.business.entity.dto.OrderTrackingStatusOptionUpdateDTO;
import com.stonebridge.quotesystem.business.entity.vo.OrderTrackingOptionVO;
import com.stonebridge.quotesystem.business.entity.vo.OrderTrackingStatusOptionVO;
import com.stonebridge.quotesystem.business.service.IOrderTrackingOptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    /**
     * 查看当前启用的订单追踪状态基础数据。
     */
    @PreAuthorize("hasAuthority('order-tracking:page')")
    @GetMapping("/order-tracking/options/statuses")
    public Result<List<OrderTrackingStatusOptionVO>> statusOptions() {
        return Result.success(orderTrackingOptionService.getStatusOptions());
    }

    /**
     * 修改状态码、显示名称、排序和标签样式。
     */
    @PreAuthorize("hasAuthority('order-tracking:update')")
    @PutMapping("/order-tracking/options/statuses/{id}")
    public Result<OrderTrackingStatusOptionVO> updateStatusOption(
            @PathVariable String id,
            @Valid @RequestBody OrderTrackingStatusOptionUpdateDTO dto) {
        return Result.success(
                "状态配置更新成功",
                orderTrackingOptionService.updateStatusOption(id, dto));
    }
}
