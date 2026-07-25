package com.stonebridge.quotesystem.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.stonebridge.quotesystem.business.entity.OrderTracking;
import com.stonebridge.quotesystem.business.entity.OrderTrackingOption;
import com.stonebridge.quotesystem.business.entity.vo.OrderTrackingListVO;
import com.stonebridge.quotesystem.business.entity.vo.OrderTrackingOptionTranslateVO;
import com.stonebridge.quotesystem.business.mapper.OrderTrackingOptionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderTrackingOptionTranslateServiceImplTest {

    @Mock
    private OrderTrackingOptionMapper optionMapper;

    @Test
    void shouldTranslateDisabledHistoricalStatusButRejectItForNewFlow() {
        when(optionMapper.selectList(any(QueryWrapper.class))).thenReturn(new ArrayList<>(List.of(
                statusOption("CARTON", null, 1100, "客户尚未提供唛头/设计资料", 10, 1),
                statusOption("CARTON", null, 1250, "设计初步完成，已发客户待确认", 30, 1),
                statusOption("CARTON", null, 1500, "检查无误", 60, 0)
        )));

        OrderTrackingOptionTranslateServiceImpl service =
                new OrderTrackingOptionTranslateServiceImpl(optionMapper);

        OrderTrackingOptionTranslateVO historical =
                service.translate("CARTON", "STATUS", null, 1500);

        assertEquals("检查无误", historical.getLabel());
        assertTrue(historical.getMatched());
        assertFalse(historical.getValidForParent());
        assertFalse(service.isValidStatusForParent("CARTON", null, 1500));
        assertTrue(service.isValidStatusForParent("CARTON", null, 1250));
        assertEquals(1100, service.getFirstStatusValue("CARTON", null));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<QueryWrapper<OrderTrackingOption>> wrapperCaptor =
                ArgumentCaptor.forClass(QueryWrapper.class);
        verify(optionMapper).selectList(wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("is_deleted"));
        assertFalse(sqlSegment.contains("is_enabled"));
    }

    @Test
    void shouldKeepIdAndUpdateTimeInListView() {
        when(optionMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(new ArrayList<>());

        OrderTrackingOptionTranslateServiceImpl service =
                new OrderTrackingOptionTranslateServiceImpl(optionMapper);
        OrderTracking order = new OrderTracking();
        order.setId("order-id");
        order.setUpdateTime(LocalDateTime.of(2026, 7, 24, 20, 30));

        OrderTrackingListVO vo = service.toListVO(order);

        assertEquals("order-id", vo.getId());
        assertEquals(order.getUpdateTime(), vo.getUpdateTime());
    }

    private OrderTrackingOption statusOption(String moduleType, Integer parentValue,
                                             Integer value, String label,
                                             Integer sortNo, Integer enabled) {
        OrderTrackingOption option = new OrderTrackingOption();
        option.setModuleType(moduleType);
        option.setOptionType("STATUS");
        option.setParentValue(parentValue);
        option.setOptionValue(value);
        option.setOptionLabel(label);
        option.setSortNo(sortNo);
        option.setTagType("warning");
        option.setIsEnabled(enabled);
        option.setIsDeleted(0);
        return option;
    }
}
