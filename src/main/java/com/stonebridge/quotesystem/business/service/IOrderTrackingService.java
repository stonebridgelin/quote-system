package com.stonebridge.quotesystem.business.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stonebridge.quotesystem.business.entity.dto.OrderTrackingModuleUpdateDTO;
import com.stonebridge.quotesystem.business.entity.dto.OrderTrackingPageQueryDTO;
import com.stonebridge.quotesystem.business.entity.dto.OrderTrackingSaveDTO;
import com.stonebridge.quotesystem.business.entity.dto.OrderTrackingWaitingDTO;
import com.stonebridge.quotesystem.business.entity.vo.OrderTrackingDetailVO;
import com.stonebridge.quotesystem.business.entity.vo.OrderTrackingListVO;
import com.stonebridge.quotesystem.business.entity.vo.OrderTrackingLogVO;

import java.util.List;

public interface IOrderTrackingService {

    Page<OrderTrackingListVO> page(OrderTrackingPageQueryDTO queryDTO);

    void create(OrderTrackingSaveDTO dto);

    void update(OrderTrackingSaveDTO dto);

    void updateModule(OrderTrackingModuleUpdateDTO dto);

    void updateModuleWaiting(OrderTrackingWaitingDTO dto);

    OrderTrackingDetailVO detail(String id);

    List<OrderTrackingLogVO> getModuleLogs(String orderId, String moduleType);

    void delete(String id);
}
