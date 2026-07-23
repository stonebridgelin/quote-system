package com.stonebridge.quotesystem.business.service;

import com.stonebridge.quotesystem.business.entity.vo.OrderTrackingOptionVO;

import java.util.List;
import java.util.Map;

public interface IOrderTrackingOptionService {

    Map<String, List<OrderTrackingOptionVO>> getOptionMap();
}