package com.stonebridge.quotesystem.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.stonebridge.quotesystem.business.entity.OrderTrackingOption;
import com.stonebridge.quotesystem.business.entity.dto.OrderTrackingStatusOptionUpdateDTO;
import com.stonebridge.quotesystem.business.entity.vo.OrderTrackingStatusOptionVO;
import com.stonebridge.quotesystem.business.mapper.OrderTrackingOptionMapper;
import com.stonebridge.quotesystem.business.service.IOrderTrackingOptionTranslateService;
import com.stonebridge.quotesystem.security.entity.SecurityUser;
import com.stonebridge.quotesystem.system.entity.SysUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderTrackingOptionServiceImplTest {

    @Mock
    private OrderTrackingOptionMapper optionMapper;
    @Mock
    private IOrderTrackingOptionTranslateService optionTranslateService;

    @BeforeEach
    void setUp() {
        SysUser user = new SysUser();
        user.setId("user-id");
        user.setUsername("lin");
        user.setStatus(1);
        SecurityUser principal =
                new SecurityUser(user, List.of("salesman"), List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldOnlyExposeEnabledOptionsToFrontend() {
        when(optionMapper.selectList(any(QueryWrapper.class))).thenReturn(new ArrayList<>());

        OrderTrackingOptionServiceImpl service =
                new OrderTrackingOptionServiceImpl(
                        optionMapper, optionTranslateService);
        service.getOptionMap();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<QueryWrapper<OrderTrackingOption>> wrapperCaptor =
                ArgumentCaptor.forClass(QueryWrapper.class);
        verify(optionMapper).selectList(wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("is_deleted"));
        assertTrue(sqlSegment.contains("is_enabled"));
    }

    @Test
    void shouldUpdateStatusOptionAndRefreshTranslationCache() {
        OrderTrackingOption status = option(
                "status-1", "STICKER", "STATUS", 30,
                3200, "客户已提供设计资料", 20);
        OrderTrackingOption type = option(
                "type-1", "STICKER", "TYPE", null,
                30, "工厂生产", 30);

        when(optionMapper.selectById("status-1")).thenReturn(status);
        when(optionMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        when(optionMapper.updateById(status)).thenReturn(1);
        when(optionMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(new ArrayList<>(List.of(type, status)));

        OrderTrackingStatusOptionUpdateDTO dto =
                new OrderTrackingStatusOptionUpdateDTO();
        dto.setOptionValue(3210);
        dto.setOptionLabel("设计资料已接收");
        dto.setSortNo(25);
        dto.setTagType("primary");
        dto.setRemark("前端可维护");

        OrderTrackingOptionServiceImpl service =
                new OrderTrackingOptionServiceImpl(
                        optionMapper, optionTranslateService);
        OrderTrackingStatusOptionVO result =
                service.updateStatusOption("status-1", dto);

        assertEquals(3210, status.getOptionValue());
        assertEquals("设计资料已接收", status.getOptionLabel());
        assertEquals(25, status.getSortNo());
        assertEquals("primary", status.getTagType());
        assertEquals("user-id", status.getUpdateBy());
        assertNotNull(status.getUpdateTime());
        assertEquals("不干胶", result.getModuleLabel());
        assertEquals("工厂生产", result.getParentLabel());
        assertEquals(3210, result.getOptionValue());
        verify(optionTranslateService, times(1)).refreshCache();
    }

    private OrderTrackingOption option(
            String id,
            String moduleType,
            String optionType,
            Integer parentValue,
            Integer optionValue,
            String optionLabel,
            Integer sortNo) {
        OrderTrackingOption option = new OrderTrackingOption();
        option.setId(id);
        option.setModuleType(moduleType);
        option.setOptionType(optionType);
        option.setParentValue(parentValue);
        option.setOptionValue(optionValue);
        option.setOptionLabel(optionLabel);
        option.setSortNo(sortNo);
        option.setTagType("warning");
        option.setIsEnabled(1);
        option.setIsDeleted(0);
        return option;
    }
}
