package com.stonebridge.quotesystem.business.controller;

import com.stonebridge.quotesystem.business.entity.dto.SignedOrderCancelDTO;
import com.stonebridge.quotesystem.business.entity.dto.SignedOrderPageQueryDTO;
import com.stonebridge.quotesystem.business.entity.dto.SignedOrderSaveDTO;
import com.stonebridge.quotesystem.business.entity.vo.CustomerOptionVO;
import com.stonebridge.quotesystem.business.entity.vo.SalesmanOptionVO;
import com.stonebridge.quotesystem.business.entity.vo.SignedOrderDetailVO;
import com.stonebridge.quotesystem.business.entity.vo.SignedOrderPageVO;
import com.stonebridge.quotesystem.business.service.ISignedOrderService;
import com.stonebridge.quotesystem.common.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/signed-order")
@RequiredArgsConstructor
public class SignedOrderController {

    private final ISignedOrderService signedOrderService;

    @PreAuthorize("hasAuthority('signed-order:list')")
    @PostMapping("/page")
    public Result<SignedOrderPageVO> page(
            @Valid @RequestBody SignedOrderPageQueryDTO queryDTO) {
        return Result.success(signedOrderService.page(queryDTO));
    }

    @PreAuthorize("hasAuthority('signed-order:create')")
    @PostMapping("/create")
    public Result<String> create(@Valid @RequestBody SignedOrderSaveDTO dto) {
        return Result.success("订单保存成功", signedOrderService.create(dto));
    }

    @PreAuthorize("hasAuthority('signed-order:detail')")
    @GetMapping("/detail/{id}")
    public Result<SignedOrderDetailVO> detail(@PathVariable String id) {
        return Result.success(signedOrderService.detail(id));
    }

    @PreAuthorize("hasAuthority('signed-order:cancel')")
    @PostMapping("/cancel/{id}")
    public Result<Void> cancel(@PathVariable String id,
                               @Valid @RequestBody SignedOrderCancelDTO dto) {
        signedOrderService.cancel(id, dto.getCancelReason());
        return Result.success();
    }

    @PreAuthorize("hasAuthority('signed-order:customer-options')")
    @GetMapping("/customer/options")
    public Result<List<CustomerOptionVO>> customerOptions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        return Result.success(signedOrderService.customerOptions(keyword, limit));
    }

    @PreAuthorize("hasAuthority('signed-order:salesman-options')")
    @GetMapping("/salesman/options")
    public Result<List<SalesmanOptionVO>> salesmanOptions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        return Result.success(signedOrderService.salesmanOptions(keyword, limit));
    }
}
