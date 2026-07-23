package com.stonebridge.quotesystem.business.service;

import com.stonebridge.quotesystem.business.entity.OrderTracking;
import com.stonebridge.quotesystem.business.entity.dto.OrderTrackingOptionTranslateDTO;
import com.stonebridge.quotesystem.business.entity.vo.OrderTrackingListVO;
import com.stonebridge.quotesystem.business.entity.vo.OrderTrackingOptionTranslateVO;

import java.util.List;

public interface IOrderTrackingOptionTranslateService {

    /**
     * 把订单追踪实体翻译成列表 VO。
     */
    OrderTrackingListVO toListVO(OrderTracking order);

    /**
     * 单个配置值翻译。
     */
    OrderTrackingOptionTranslateVO translate(String moduleType, String optionType, Integer parentValue, Integer value);

    /**
     * 批量配置值翻译。
     */
    List<OrderTrackingOptionTranslateVO> translateBatch(List<OrderTrackingOptionTranslateDTO> requests);

    /**
     * 判断某状态是否属于某模块某类型。
     */
    boolean isValidStatusForParent(String moduleType, Integer parentValue, Integer statusValue);

    /**
     * 读取某模块某类型下的第一个状态值，用于类型变化后自动纠正非法状态。
     */
    Integer getFirstStatusValue(String moduleType, Integer parentValue);

    /**
     * 手动刷新翻译缓存。
     */
    void refreshCache();
}
