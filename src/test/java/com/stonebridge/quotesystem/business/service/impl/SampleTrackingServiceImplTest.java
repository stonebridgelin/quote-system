package com.stonebridge.quotesystem.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stonebridge.quotesystem.business.entity.SampleImage;
import com.stonebridge.quotesystem.business.entity.SampleTracking;
import com.stonebridge.quotesystem.business.entity.dto.SampleSaveDTO;
import com.stonebridge.quotesystem.business.mapper.SampleImageMapper;
import com.stonebridge.quotesystem.business.mapper.SampleTrackingMapper;
import com.stonebridge.quotesystem.exception.BusinessException;
import com.stonebridge.quotesystem.security.entity.SecurityUser;
import com.stonebridge.quotesystem.system.entity.SysUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SampleTrackingServiceImplTest {

    @Mock
    private SampleTrackingMapper trackingMapper;
    @Mock
    private SampleImageMapper imageMapper;

    private SampleTrackingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SampleTrackingServiceImpl(trackingMapper, imageMapper);

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
    void shouldCreateAsMakingAndIgnoreRequestedFinalStatus() {
        when(trackingMapper.insert(any(SampleTracking.class))).thenAnswer(invocation -> {
            SampleTracking tracking = invocation.getArgument(0);
            tracking.setId(1L);
            return 1;
        });

        SampleSaveDTO dto = new SampleSaveDTO();
        dto.setCustomerInfo("客户A");
        dto.setPlanDate(LocalDate.now().plusDays(3));
        dto.setRemarks("首次制样");
        dto.setStatus("SHIPPED");
        dto.setTrackingNo("SF001");

        service.saveOrUpdate(dto);

        ArgumentCaptor<SampleTracking> captor = ArgumentCaptor.forClass(SampleTracking.class);
        verify(trackingMapper).insert(captor.capture());
        SampleTracking saved = captor.getValue();
        assertEquals("MAKING", saved.getStatus());
        assertEquals("user-id", saved.getCreator());
        assertNull(saved.getTrackingNo());
        assertNull(saved.getEndTime());
        assertNotNull(saved.getCreateTime());
        assertEquals(saved.getCreateTime(), saved.getUpdateTime());
    }

    @Test
    void shouldCompleteWhenMakingRecordIsShipped() {
        SampleTracking existing = tracking(2L, "MAKING");
        when(trackingMapper.selectById(2L)).thenReturn(existing);

        SampleSaveDTO dto = updateDto(2L, "SHIPPED");
        dto.setTrackingNo("  SF002  ");

        service.saveOrUpdate(dto);

        assertEquals("SHIPPED", existing.getStatus());
        assertEquals("SF002", existing.getTrackingNo());
        assertNotNull(existing.getEndTime());
        assertEquals(existing.getEndTime(), existing.getUpdateTime());
        verify(trackingMapper).updateById(existing);
    }

    @Test
    void shouldRequireTrackingNumberWhenShipping() {
        SampleTracking existing = tracking(3L, "MAKING");
        when(trackingMapper.selectById(3L)).thenReturn(existing);

        SampleSaveDTO dto = updateDto(3L, "SHIPPED");
        dto.setTrackingNo(" ");

        BusinessException exception = assertThrows(
                BusinessException.class, () -> service.saveOrUpdate(dto));

        assertEquals("快递单号不能为空", exception.getMessage());
        verify(trackingMapper, never()).updateById(any(SampleTracking.class));
        verify(imageMapper, never()).delete(any());
    }

    @Test
    void shouldPreserveCompletionTimeWhenEditingShippedRecord() {
        LocalDateTime originalEndTime = LocalDateTime.of(2026, 7, 20, 10, 30);
        SampleTracking existing = tracking(4L, "SHIPPED");
        existing.setTrackingNo("SF004");
        existing.setEndTime(originalEndTime);
        when(trackingMapper.selectById(4L)).thenReturn(existing);

        SampleSaveDTO dto = updateDto(4L, "SHIPPED");
        dto.setRemarks("补充图片和备注");
        dto.setTrackingNo("SF004-NEW");
        dto.setImages(List.of("image-a", "image-b"));

        service.saveOrUpdate(dto);

        assertSame(originalEndTime, existing.getEndTime());
        assertEquals("SHIPPED", existing.getStatus());
        assertEquals("SF004-NEW", existing.getTrackingNo());
        assertEquals("补充图片和备注", existing.getRemarks());
        ArgumentCaptor<SampleImage> imageCaptor = ArgumentCaptor.forClass(SampleImage.class);
        verify(imageMapper, org.mockito.Mockito.times(2)).insert(imageCaptor.capture());
        assertTrue(imageCaptor.getAllValues().stream()
                .allMatch(image -> Long.valueOf(4L).equals(image.getSampleId())));
    }

    @Test
    void shouldKeepEndedAsCompatibleCompletionStatus() {
        SampleTracking existing = tracking(5L, "MAKING");
        when(trackingMapper.selectById(5L)).thenReturn(existing);

        service.saveOrUpdate(updateDto(5L, "ENDED"));

        assertEquals("ENDED", existing.getStatus());
        assertNotNull(existing.getEndTime());
        assertEquals(existing.getEndTime(), existing.getUpdateTime());
    }

    @ParameterizedTest
    @ValueSource(strings = {"SHIPPED", "ENDED"})
    void shouldRejectReturningCompletedRecordToMaking(String currentStatus) {
        SampleTracking existing = tracking(6L, currentStatus);
        existing.setEndTime(LocalDateTime.of(2026, 7, 21, 9, 0));
        when(trackingMapper.selectById(6L)).thenReturn(existing);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.saveOrUpdate(updateDto(6L, "MAKING")));

        assertEquals("已完成的样品单不能退回制作中", exception.getMessage());
        verify(trackingMapper, never()).updateById(any(SampleTracking.class));
    }

    @Test
    void shouldRejectUnknownStatus() {
        SampleTracking existing = tracking(7L, "MAKING");
        when(trackingMapper.selectById(7L)).thenReturn(existing);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.saveOrUpdate(updateDto(7L, "DONE")));

        assertEquals("样品状态不合法", exception.getMessage());
        verify(trackingMapper, never()).updateById(any(SampleTracking.class));
    }

    @Test
    void shouldGroupShippedAndEndedTogetherAndSortByCompletionFallbackTime() {
        SampleTracking shipped = tracking(8L, "SHIPPED");
        shipped.setEndTime(LocalDateTime.of(2026, 7, 20, 10, 0));
        shipped.setUpdateTime(LocalDateTime.of(2026, 7, 23, 10, 0));

        SampleTracking ended = tracking(9L, "ENDED");
        ended.setEndTime(null);
        ended.setUpdateTime(LocalDateTime.of(2026, 7, 22, 10, 0));

        when(trackingMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(new ArrayList<>(List.of(shipped, ended)));

        Page<SampleTracking> page = service.getListPage(
                1, 20, null, List.of("SHIPPED", "ENDED"));

        assertEquals(List.of(9L, 8L),
                page.getRecords().stream().map(SampleTracking::getId).toList());
        assertTrue(page.getRecords().stream()
                .allMatch(item -> Integer.valueOf(3).equals(item.getSortGroup())));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<QueryWrapper<SampleTracking>> wrapperCaptor =
                ArgumentCaptor.forClass(QueryWrapper.class);
        verify(trackingMapper).selectList(wrapperCaptor.capture());
        QueryWrapper<SampleTracking> wrapper = wrapperCaptor.getValue();
        assertTrue(wrapper.getSqlSegment().contains("status IN"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue("SHIPPED"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue("ENDED"));
    }

    @Test
    void shouldPropagateImageFailureAndDeclareExceptionRollback() throws Exception {
        SampleTracking existing = tracking(10L, "MAKING");
        when(trackingMapper.selectById(10L)).thenReturn(existing);
        when(imageMapper.insert(any(SampleImage.class)))
                .thenThrow(new IllegalStateException("图片保存失败"));

        SampleSaveDTO dto = updateDto(10L, "MAKING");
        dto.setImages(List.of("broken-image"));

        assertThrows(IllegalStateException.class, () -> service.saveOrUpdate(dto));
        verify(trackingMapper).updateById(existing);

        Method method = SampleTrackingServiceImpl.class
                .getMethod("saveOrUpdate", SampleSaveDTO.class);
        Transactional transactional = method.getAnnotation(Transactional.class);
        assertNotNull(transactional);
        assertArrayEquals(new Class<?>[]{Exception.class}, transactional.rollbackFor());
    }

    private SampleSaveDTO updateDto(Long id, String status) {
        SampleSaveDTO dto = new SampleSaveDTO();
        dto.setId(id);
        dto.setStatus(status);
        return dto;
    }

    private SampleTracking tracking(Long id, String status) {
        SampleTracking tracking = new SampleTracking();
        tracking.setId(id);
        tracking.setStatus(status);
        tracking.setPlanDate(LocalDate.now().plusDays(1));
        tracking.setCreateTime(LocalDateTime.of(2026, 7, 18, 8, 0));
        tracking.setUpdateTime(LocalDateTime.of(2026, 7, 18, 8, 0));
        return tracking;
    }
}
