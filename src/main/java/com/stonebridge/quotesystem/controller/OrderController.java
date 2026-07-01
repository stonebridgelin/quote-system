package com.stonebridge.quotesystem.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stonebridge.quotesystem.common.Result;
import com.stonebridge.quotesystem.entity.OrderMain;
import com.stonebridge.quotesystem.entity.dto.OrderSaveDTO;
import com.stonebridge.quotesystem.entity.dto.RecordSaveDTO;
import com.stonebridge.quotesystem.service.IOrderMainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order")
@CrossOrigin
public class OrderController {

    @Autowired
    private IOrderMainService orderService;

    @GetMapping("/list")
    public Result<Page<OrderMain>> getList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "15") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String deliveryDate) {
        return Result.success(orderService.getOrderPage(current, size, keyword, status, deliveryDate));
    }

    @GetMapping("/detail/{id}")
    public Result<OrderSaveDTO> getDetail(@PathVariable String id) {
        return Result.success(orderService.getOrderDetail(id));
    }

    @PostMapping("/save")
    public Result<String> saveOrder(@RequestBody OrderSaveDTO dto) {
        orderService.saveOrder(dto);
        return Result.success("保存成功");
    }

    @PostMapping("/record/save")
    public Result<String> saveRecord(@RequestBody RecordSaveDTO dto) {
        orderService.saveRecord(dto);
        return Result.success("记录添加成功");
    }

    @GetMapping("/record/list/{orderId}")
    public Result<List<RecordSaveDTO>> getRecordList(@PathVariable String orderId) {
        return Result.success(orderService.getRecordList(orderId));
    }

    @DeleteMapping("/delete/{id}")
    public Result<String> deleteOrder(@PathVariable String id) {
        // 由于配置了 @TableLogic，这里底层会自动执行 UPDATE t_order_main SET del_flag = 1 WHERE id = ?
        orderService.removeById(id); // 或者 mainMapper.deleteById(id);
        return Result.success("删除成功");
    }
}