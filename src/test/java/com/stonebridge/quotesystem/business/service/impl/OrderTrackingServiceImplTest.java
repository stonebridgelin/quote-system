package com.stonebridge.quotesystem.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stonebridge.quotesystem.business.entity.OrderTracking;
import com.stonebridge.quotesystem.business.entity.OrderTrackingLog;
import com.stonebridge.quotesystem.business.entity.OrderTrackingLogImage;
import com.stonebridge.quotesystem.business.entity.dto.OrderTrackingModuleUpdateDTO;
import com.stonebridge.quotesystem.business.entity.dto.OrderTrackingPageQueryDTO;
import com.stonebridge.quotesystem.business.entity.dto.OrderTrackingSaveDTO;
import com.stonebridge.quotesystem.business.entity.dto.OrderTrackingWaitingDTO;
import com.stonebridge.quotesystem.business.entity.vo.OrderTrackingDetailVO;
import com.stonebridge.quotesystem.business.entity.vo.OrderTrackingListVO;
import com.stonebridge.quotesystem.business.entity.vo.OrderTrackingLogVO;
import com.stonebridge.quotesystem.business.mapper.OrderTrackingLogImageMapper;
import com.stonebridge.quotesystem.business.mapper.OrderTrackingLogMapper;
import com.stonebridge.quotesystem.business.mapper.OrderTrackingMapper;
import com.stonebridge.quotesystem.business.service.IOrderTrackingOptionTranslateService;
import com.stonebridge.quotesystem.security.entity.SecurityUser;
import com.stonebridge.quotesystem.system.entity.SysUser;
import com.stonebridge.quotesystem.system.service.SysUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderTrackingServiceImplTest {

    @Mock
    private OrderTrackingMapper orderTrackingMapper;
    @Mock
    private OrderTrackingLogMapper orderTrackingLogMapper;
    @Mock
    private OrderTrackingLogImageMapper orderTrackingLogImageMapper;
    @Mock
    private IOrderTrackingOptionTranslateService optionTranslateService;
    @Mock
    private SysUserService sysUserService;

    private OrderTrackingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrderTrackingServiceImpl(
                orderTrackingMapper,
                orderTrackingLogMapper,
                orderTrackingLogImageMapper,
                optionTranslateService,
                sysUserService);

        SysUser user = new SysUser();
        user.setId("user-id");
        user.setUsername("lin");
        user.setStatus(1);
        SecurityUser principal = new SecurityUser(user, List.of("salesman"), List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldOrderDatabasePageByUpdateTimeAndIdDescending() {
        OrderTracking entity = ownedOrder();
        Page<OrderTracking> rawPage = new Page<>(1, 10, 1);
        rawPage.setRecords(List.of(entity));
        when(orderTrackingMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
                .thenReturn(rawPage);

        OrderTrackingListVO listVO = new OrderTrackingListVO();
        listVO.setId(entity.getId());
        listVO.setUpdateTime(entity.getUpdateTime());
        listVO.setCreateBy("user-id");
        listVO.setUpdateBy("updater-id");
        when(optionTranslateService.toListVO(entity)).thenReturn(listVO);
        when(sysUserService.listByIds(anyCollection())).thenReturn(List.of(
                user("user-id", "lin", null, null),
                user("updater-id", "admin", null, "管理员")
        ));

        Page<OrderTrackingListVO> result = service.page(new OrderTrackingPageQueryDTO());

        assertEquals(entity.getId(), result.getRecords().get(0).getId());
        assertEquals(entity.getUpdateTime(), result.getRecords().get(0).getUpdateTime());
        assertEquals("lin", result.getRecords().get(0).getCreateBy());
        assertEquals("admin", result.getRecords().get(0).getUpdateBy());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> userIdsCaptor =
                ArgumentCaptor.forClass(Collection.class);
        verify(sysUserService, times(1)).listByIds(userIdsCaptor.capture());
        assertEquals(Set.of("user-id", "updater-id"), Set.copyOf(userIdsCaptor.getValue()));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<QueryWrapper<OrderTracking>> wrapperCaptor =
                ArgumentCaptor.forClass(QueryWrapper.class);
        verify(orderTrackingMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        String sql = normalizeSql(wrapperCaptor.getValue().getSqlSegment());
        assertTrue(sql.endsWith("order by update_time desc,id desc"));
    }

    @Test
    void shouldReturnModuleLogsInAscendingOrder() {
        when(orderTrackingMapper.selectById("order-1")).thenReturn(ownedOrder());

        OrderTrackingLog log = new OrderTrackingLog();
        log.setId("log-1");
        log.setOrderId("order-1");
        log.setModuleType("CARTON");
        log.setSortNo(1L);
        log.setCreateTime(LocalDateTime.of(2026, 7, 24, 10, 0));
        log.setCreateBy("user-id");
        log.setBeforeStatus(1100);
        log.setAfterStatus(1200);
        log.setRemarkContent("客户提供设计资料");
        when(orderTrackingLogMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(new ArrayList<>(List.of(log)));
        when(orderTrackingLogImageMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
        when(orderTrackingLogImageMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(new ArrayList<>());
        when(sysUserService.listByIds(anyCollection()))
                .thenReturn(List.of(user("user-id", "lin", null, null)));

        List<OrderTrackingLogVO> result = service.getModuleLogs("order-1", "CARTON");

        assertEquals(1, result.size());
        assertEquals("log-1", result.get(0).getId());
        assertEquals(1L, result.get(0).getSortNo());
        assertEquals("lin", result.get(0).getCreateBy());
        assertEquals(List.of(), result.get(0).getImages());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<QueryWrapper<OrderTrackingLog>> wrapperCaptor =
                ArgumentCaptor.forClass(QueryWrapper.class);
        verify(orderTrackingLogMapper).selectList(wrapperCaptor.capture());
        String sql = normalizeSql(wrapperCaptor.getValue().getSqlSegment());
        assertTrue(sql.endsWith("order by sort_no asc,create_time asc,id asc"));
    }

    @Test
    void shouldOnlyReturnOrderRecentLogsInDetail() {
        when(orderTrackingMapper.selectById("order-1")).thenReturn(ownedOrder());
        when(orderTrackingLogMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(new ArrayList<>());

        OrderTrackingDetailVO detail = service.detail("order-1");

        assertEquals(Set.of("ORDER"), detail.getRecentLogs().keySet());
        assertEquals(List.of(), detail.getRecentLogs().get("ORDER"));
        verify(orderTrackingLogMapper, times(1)).selectList(any(QueryWrapper.class));
    }

    @Test
    void shouldRefreshUpdateTimeForAllModificationEntrypoints() {
        OrderTracking oldOrder = ownedOrder();
        when(orderTrackingMapper.selectById("order-1")).thenReturn(oldOrder);

        OrderTrackingSaveDTO updateDTO = unchangedUpdateDTO();
        service.update(updateDTO);
        assertLastUpdatedOrderHasTimestamp();

        OrderTrackingModuleUpdateDTO moduleDTO = new OrderTrackingModuleUpdateDTO();
        moduleDTO.setOrderId("order-1");
        moduleDTO.setModuleType("ORDER");
        service.updateModule(moduleDTO);
        assertLastUpdatedOrderHasTimestamp();

        OrderTrackingWaitingDTO waitingDTO = new OrderTrackingWaitingDTO();
        waitingDTO.setOrderId("order-1");
        waitingDTO.setModuleType("STICKER");
        waitingDTO.setWaiting(1);
        when(orderTrackingLogMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
        service.updateModuleWaiting(waitingDTO);
        assertLastUpdatedOrderHasTimestamp();

        verify(orderTrackingMapper, times(3)).updateById(any(OrderTracking.class));
    }

    private void assertLastUpdatedOrderHasTimestamp() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<OrderTracking> captor = ArgumentCaptor.forClass(OrderTracking.class);
        verify(orderTrackingMapper, org.mockito.Mockito.atLeastOnce()).updateById(captor.capture());
        OrderTracking updated = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertNotNull(updated.getUpdateTime());
        assertEquals("user-id", updated.getUpdateBy());
    }

    private OrderTrackingSaveDTO unchangedUpdateDTO() {
        OrderTrackingSaveDTO dto = new OrderTrackingSaveDTO();
        dto.setId("order-1");
        dto.setBottomLabel(0);
        dto.setSticker(0);
        dto.setPrinting(0);
        dto.setInnerBox(0);
        dto.setColorBox(0);
        dto.setCartonStatus(1100);
        dto.setIsBottomLabelWaiting(0);
        dto.setIsStickerWaiting(0);
        dto.setIsPrintingWaiting(0);
        dto.setIsInnerBoxWaiting(0);
        dto.setIsColorBoxWaiting(0);
        dto.setIsCartonWaiting(0);
        dto.setAntiCutBoard(0);
        dto.setIsFinished(0);
        return dto;
    }

    private OrderTracking ownedOrder() {
        OrderTracking order = new OrderTracking();
        order.setId("order-1");
        order.setOrderNo("CT-001");
        order.setCreateBy("user-id");
        order.setIsDeleted(0);
        order.setIsFinished(0);
        order.setBottomLabel(0);
        order.setSticker(0);
        order.setPrinting(0);
        order.setInnerBox(0);
        order.setColorBox(0);
        order.setCartonStatus(1100);
        order.setIsBottomLabelWaiting(0);
        order.setIsStickerWaiting(0);
        order.setIsPrintingWaiting(0);
        order.setIsInnerBoxWaiting(0);
        order.setIsColorBoxWaiting(0);
        order.setIsCartonWaiting(0);
        order.setAntiCutBoard(0);
        order.setCreateTime(LocalDateTime.of(2026, 7, 20, 9, 0));
        order.setUpdateTime(LocalDateTime.of(2026, 7, 24, 20, 30));
        return order;
    }

    private SysUser user(String id, String username, String realName, String nickname) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        user.setRealName(realName);
        user.setNickname(nickname);
        return user;
    }

    private String normalizeSql(String sql) {
        return sql == null ? "" : sql.trim().replaceAll("\\s+", " ").toLowerCase();
    }
}
