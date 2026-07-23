package com.stonebridge.quotesystem.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.stonebridge.quotesystem.business.entity.OrderTrackingOption;
import com.stonebridge.quotesystem.business.entity.vo.OrderTrackingOptionVO;
import com.stonebridge.quotesystem.business.mapper.OrderTrackingOptionMapper;
import com.stonebridge.quotesystem.business.service.IOrderTrackingOptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderTrackingOptionServiceImpl implements IOrderTrackingOptionService {

    private final OrderTrackingOptionMapper orderTrackingOptionMapper;

    @Override
    public Map<String, List<OrderTrackingOptionVO>> getOptionMap() {
        List<OrderTrackingOption> list = orderTrackingOptionMapper.selectList(
                new QueryWrapper<OrderTrackingOption>()
                        .eq("is_deleted", 0)
                        .eq("is_enabled", 1)
                        .orderByAsc("module_type")
                        .orderByAsc("option_type")
                        .orderByAsc("parent_value")
                        .orderByAsc("sort_no")
        );

        List<OrderTrackingOptionVO> voList = list.stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        Map<String, List<OrderTrackingOptionVO>> result = voList.stream()
                .collect(Collectors.groupingBy(
                        item -> buildKey(item.getModuleType(), item.getOptionType(), item.getParentValue()),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // 结构优化：额外提供 MODULE_STATUS_ALL 兜底分组。
        // 列表页已改为后端 VO 翻译，但表单或其他动态页面仍可使用 ALL 做前端兜底。
        Map<String, List<OrderTrackingOptionVO>> statusAllMap = voList.stream()
                .filter(item -> "STATUS".equals(item.getOptionType()))
                .collect(Collectors.groupingBy(
                        item -> item.getModuleType() + "_STATUS_ALL",
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        statusAllMap.forEach((key, value) -> result.put(key, new ArrayList<>(value)));

        return result;
    }

    private OrderTrackingOptionVO toVO(OrderTrackingOption option) {
        OrderTrackingOptionVO vo = new OrderTrackingOptionVO();
        BeanUtils.copyProperties(option, vo);
        vo.setValue(option.getOptionValue());
        vo.setLabel(option.getOptionLabel());
        return vo;
    }

    private String buildKey(String moduleType, String optionType, Integer parentValue) {
        if ("STATUS".equals(optionType) && parentValue != null) {
            return moduleType + "_" + optionType + "_" + parentValue;
        }
        return moduleType + "_" + optionType;
    }
}
