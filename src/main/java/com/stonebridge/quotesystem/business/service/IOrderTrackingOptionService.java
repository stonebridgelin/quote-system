package com.stonebridge.quotesystem.business.service;

import com.stonebridge.quotesystem.business.entity.dto.OrderTrackingStatusOptionUpdateDTO;
import com.stonebridge.quotesystem.business.entity.vo.OrderTrackingOptionVO;
import com.stonebridge.quotesystem.business.entity.vo.OrderTrackingStatusOptionVO;

import java.util.List;
import java.util.Map;

public interface IOrderTrackingOptionService {

    Map<String, List<OrderTrackingOptionVO>> getOptionMap();

    List<OrderTrackingStatusOptionVO> getStatusOptions();

    OrderTrackingStatusOptionVO updateStatusOption(
            String id,
            OrderTrackingStatusOptionUpdateDTO dto);
}
