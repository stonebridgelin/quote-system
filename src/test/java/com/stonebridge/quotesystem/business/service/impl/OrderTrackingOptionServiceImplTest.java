package com.stonebridge.quotesystem.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.stonebridge.quotesystem.business.entity.OrderTrackingOption;
import com.stonebridge.quotesystem.business.mapper.OrderTrackingOptionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderTrackingOptionServiceImplTest {

    @Mock
    private OrderTrackingOptionMapper optionMapper;

    @Test
    void shouldOnlyExposeEnabledOptionsToFrontend() {
        when(optionMapper.selectList(any(QueryWrapper.class))).thenReturn(new ArrayList<>());

        OrderTrackingOptionServiceImpl service = new OrderTrackingOptionServiceImpl(optionMapper);
        service.getOptionMap();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<QueryWrapper<OrderTrackingOption>> wrapperCaptor =
                ArgumentCaptor.forClass(QueryWrapper.class);
        verify(optionMapper).selectList(wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("is_deleted"));
        assertTrue(sqlSegment.contains("is_enabled"));
    }
}
