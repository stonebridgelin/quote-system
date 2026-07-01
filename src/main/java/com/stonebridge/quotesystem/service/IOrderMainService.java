package com.stonebridge.quotesystem.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stonebridge.quotesystem.entity.OrderMain;
import com.stonebridge.quotesystem.entity.dto.OrderSaveDTO;
import com.stonebridge.quotesystem.entity.dto.RecordSaveDTO;

import java.util.List;

public interface IOrderMainService {
    // 1. 分页高级查询
    Page<OrderMain> getOrderPage(Integer current, Integer size, String keyword, Integer status, String deliveryDate);
    // 2. 获取订单及明细详情
    OrderSaveDTO getOrderDetail(String id);
    // 3. 保存或更新订单
    void saveOrder(OrderSaveDTO dto);
    // 4. 保存跟进记录
    void saveRecord(RecordSaveDTO dto);
    // 5. 获取指定订单的历史记录时间轴
    List<RecordSaveDTO> getRecordList(String orderId);

    void removeById(String id);
}