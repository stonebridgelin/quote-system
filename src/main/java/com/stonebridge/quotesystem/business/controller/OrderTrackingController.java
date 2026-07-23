package com.stonebridge.quotesystem.business.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stonebridge.quotesystem.common.Result;
import com.stonebridge.quotesystem.business.entity.dto.OrderTrackingModuleUpdateDTO;
import com.stonebridge.quotesystem.business.entity.dto.OrderTrackingPageQueryDTO;
import com.stonebridge.quotesystem.business.entity.dto.OrderTrackingSaveDTO;
import com.stonebridge.quotesystem.business.entity.dto.OrderTrackingWaitingDTO;
import com.stonebridge.quotesystem.business.entity.vo.OrderTrackingDetailVO;
import com.stonebridge.quotesystem.business.entity.vo.OrderTrackingListVO;
import com.stonebridge.quotesystem.business.entity.vo.OrderTrackingLogVO;
import com.stonebridge.quotesystem.business.service.IOrderTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/order-tracking")
@RequiredArgsConstructor
public class OrderTrackingController {

    private final IOrderTrackingService orderTrackingService;

    /**
     * 订单追踪列表分页查询。
     * <p>
     * 结构优化：返回 OrderTrackingListVO，列表展示文本由后端统一翻译完成，
     * 前端不再负责状态码到中文的转换。
     */
    @PreAuthorize("hasAuthority('order-tracking:page')")
    @PostMapping("/page")
    public Result<Page<OrderTrackingListVO>> page(@RequestBody OrderTrackingPageQueryDTO queryDTO) {
        return Result.success(orderTrackingService.page(queryDTO));
    }

    /**
     * 新增订单追踪。
     */
    @PreAuthorize("hasAuthority('order-tracking:create')")
    @PostMapping("/create")
    public Result<Void> create(@RequestBody OrderTrackingSaveDTO dto) {
        orderTrackingService.create(dto);
        return Result.success();
    }

    /**
     * 编辑订单追踪。
     */
    @PreAuthorize("hasAuthority('order-tracking:update')")
    @PostMapping("/update")
    public Result<Void> update(@RequestBody OrderTrackingSaveDTO dto) {
        orderTrackingService.update(dto);
        return Result.success();
    }

    /**
     * 保存某一个追踪模块的本次更新。
     */
    @PreAuthorize("hasAuthority('order-tracking:update')")
    @PostMapping("/module/update")
    public Result<Void> updateModule(@RequestBody OrderTrackingModuleUpdateDTO dto) {
        orderTrackingService.updateModule(dto);
        return Result.success("保存本项更新成功", null);
    }

    /**
     * 查看订单详情。
     */
    @PreAuthorize("hasAuthority('order-tracking:detail')")
    @GetMapping("/detail/{id}")
    public Result<OrderTrackingDetailVO> detail(@PathVariable String id) {
        return Result.success(orderTrackingService.detail(id));
    }

    /**
     * 查看某个模块的完整日志。
     */
    @PreAuthorize("hasAuthority('order-tracking:detail')")
    @GetMapping("/logs")
    public Result<List<OrderTrackingLogVO>> logs(
            @RequestParam String orderId,
            @RequestParam String moduleType
    ) {
        return Result.success(orderTrackingService.getModuleLogs(orderId, moduleType));
    }

    /**
     * 逻辑删除订单。
     */
    @PreAuthorize("hasAuthority('order-tracking:delete')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        orderTrackingService.delete(id);
        return Result.success();
    }

    /**
     * 开启/解除等待回复提醒。
     */
    @PreAuthorize("hasAuthority('order-tracking:update')")
    @PostMapping("/module/waiting")
    public Result<Void> updateModuleWaiting(@RequestBody OrderTrackingWaitingDTO dto) {
        orderTrackingService.updateModuleWaiting(dto);
        return Result.success();
    }
}
