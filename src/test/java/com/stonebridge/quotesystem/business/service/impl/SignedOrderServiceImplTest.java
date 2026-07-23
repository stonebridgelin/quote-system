package com.stonebridge.quotesystem.business.service.impl;

import com.stonebridge.quotesystem.business.entity.Customer;
import com.stonebridge.quotesystem.business.entity.SignedOrderDetail;
import com.stonebridge.quotesystem.business.entity.SignedOrderMain;
import com.stonebridge.quotesystem.business.entity.SignedOrderSetItem;
import com.stonebridge.quotesystem.business.entity.dto.SignedOrderDetailSaveDTO;
import com.stonebridge.quotesystem.business.entity.dto.SignedOrderSaveDTO;
import com.stonebridge.quotesystem.business.entity.dto.SignedOrderSetItemSaveDTO;
import com.stonebridge.quotesystem.business.mapper.CustomerMapper;
import com.stonebridge.quotesystem.business.mapper.SignedOrderDetailMapper;
import com.stonebridge.quotesystem.business.mapper.SignedOrderMainMapper;
import com.stonebridge.quotesystem.business.mapper.SignedOrderSetItemMapper;
import com.stonebridge.quotesystem.exception.BusinessException;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignedOrderServiceImplTest {

    @Mock
    private CustomerMapper customerMapper;
    @Mock
    private SignedOrderMainMapper signedOrderMainMapper;
    @Mock
    private SignedOrderDetailMapper signedOrderDetailMapper;
    @Mock
    private SignedOrderSetItemMapper signedOrderSetItemMapper;
    @Mock
    private SysUserService sysUserService;

    private SignedOrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SignedOrderServiceImpl(
                customerMapper,
                signedOrderMainMapper,
                signedOrderDetailMapper,
                signedOrderSetItemMapper,
                sysUserService);

        SysUser currentUser = user("current-user-id", "lin", "林业务");
        SecurityUser principal = new SecurityUser(
                currentUser, List.of("salesman"), List.of("signed-order:create"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRecalculateSingleSetAndOrderTotals() {
        Customer customer = new Customer();
        customer.setId("customer-id");
        customer.setCustomerName("客户A");
        when(customerMapper.selectOne(any())).thenReturn(customer);

        SysUser salesman = user("salesman-id", "sales01", "张三");
        when(sysUserService.getById("salesman-id")).thenReturn(salesman);
        when(signedOrderMainMapper.insert(any(SignedOrderMain.class))).thenReturn(1);
        when(signedOrderDetailMapper.insertBatch(anyList()))
                .thenAnswer(invocation -> invocation.<List<?>>getArgument(0).size());
        when(signedOrderSetItemMapper.insertBatch(anyList()))
                .thenAnswer(invocation -> invocation.<List<?>>getArgument(0).size());

        SignedOrderSaveDTO dto = baseOrder();
        dto.setDetailList(List.of(singleLine(), setLine()));

        String orderId = service.create(dto);

        ArgumentCaptor<SignedOrderMain> mainCaptor =
                ArgumentCaptor.forClass(SignedOrderMain.class);
        verify(signedOrderMainMapper).insert(mainCaptor.capture());
        SignedOrderMain main = mainCaptor.getValue();
        assertEquals(orderId, main.getId());
        assertEquals(108L, main.getTotalPcs());
        assertEquals(10L, main.getTotalSets());
        assertEquals(new BigDecimal("262.68"), main.getTotalAmount());
        assertEquals("CNY", main.getCurrency());
        assertEquals("张三", main.getSalesmanName());
        assertEquals("lin", main.getCreator());

        ArgumentCaptor<List<SignedOrderDetail>> detailCaptor = listCaptor();
        verify(signedOrderDetailMapper).insertBatch(detailCaptor.capture());
        List<SignedOrderDetail> details = detailCaptor.getValue();
        SignedOrderDetail single = details.get(0);
        assertEquals(48L, single.getTotalPcs());
        assertEquals(0L, single.getTotalSets());
        assertEquals(new BigDecimal("2.5830"), single.getUnitPrice());
        assertEquals(new BigDecimal("123.98"), single.getAmount());

        SignedOrderDetail set = details.get(1);
        assertNull(set.getProductCode());
        assertEquals(10L, set.getTotalSets());
        assertEquals(60L, set.getTotalPcs());
        assertEquals(new BigDecimal("2020.000"), set.getWeight());
        assertEquals(new BigDecimal("138.70"), set.getAmount());

        ArgumentCaptor<List<SignedOrderSetItem>> itemCaptor = listCaptor();
        verify(signedOrderSetItemMapper).insertBatch(itemCaptor.capture());
        List<SignedOrderSetItem> items = itemCaptor.getValue();
        assertEquals(2, items.size());
        assertEquals(set.getId(), items.get(0).getOrderDetailId());
        assertEquals(new BigDecimal("410.000"), items.get(0).getWeight());
        assertEquals(2, items.get(0).getQtyPerSet());
    }

    @Test
    void shouldRejectDuplicateProductInsideSet() {
        Customer customer = new Customer();
        customer.setId("customer-id");
        customer.setCustomerName("客户A");
        when(customerMapper.selectOne(any())).thenReturn(customer);
        when(sysUserService.getById("salesman-id"))
                .thenReturn(user("salesman-id", "sales01", "张三"));

        SignedOrderDetailSaveDTO set = setLine();
        set.getSetItems().get(1).setProductCode("plate");
        SignedOrderSaveDTO dto = baseOrder();
        dto.setDetailList(List.of(set));

        BusinessException exception = assertThrows(
                BusinessException.class, () -> service.create(dto));
        assertEquals("第1条套装存在重复商品代号：plate", exception.getMessage());
    }

    private SignedOrderSaveDTO baseOrder() {
        SignedOrderSaveDTO dto = new SignedOrderSaveDTO();
        dto.setOrderNo("SO-2026-001");
        dto.setCustomerName("客户A");
        dto.setSalesmanId("salesman-id");
        dto.setCreateDate(LocalDate.of(2026, 7, 23));
        dto.setCurrency("CNY");
        dto.setRemark("测试订单");
        return dto;
    }

    private SignedOrderDetailSaveDTO singleLine() {
        SignedOrderDetailSaveDTO dto = new SignedOrderDetailSaveDTO();
        dto.setLineType("SINGLE");
        dto.setProductCode("PLATE");
        dto.setWeight(new BigDecimal("410"));
        dto.setTotalPcs(48L);
        dto.setUnitPrice(new BigDecimal("2.583"));
        return dto;
    }

    private SignedOrderDetailSaveDTO setLine() {
        SignedOrderDetailSaveDTO dto = new SignedOrderDetailSaveDTO();
        dto.setLineType("SET");
        dto.setTotalSets(10L);
        dto.setTotalPcs(9999L);
        dto.setUnitPrice(new BigDecimal("13.87"));
        dto.setSetItems(List.of(
                setItem("PLATE", "410", 2),
                setItem("BOWL", "300", 4)));
        return dto;
    }

    private SignedOrderSetItemSaveDTO setItem(String code, String weight, int qty) {
        SignedOrderSetItemSaveDTO dto = new SignedOrderSetItemSaveDTO();
        dto.setProductCode(code);
        dto.setWeight(new BigDecimal(weight));
        dto.setQtyPerSet(qty);
        return dto;
    }

    private SysUser user(String id, String username, String realName) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        user.setRealName(realName);
        user.setStatus(1);
        return user;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T> ArgumentCaptor<List<T>> listCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(List.class);
    }
}
