package com.stonebridge.quotesystem.business.service.impl;

import com.stonebridge.quotesystem.business.entity.QuoteDetail;
import com.stonebridge.quotesystem.business.entity.QuoteMain;
import com.stonebridge.quotesystem.business.entity.QuoteSetItem;
import com.stonebridge.quotesystem.business.entity.dto.QuoteExportDTO;
import com.stonebridge.quotesystem.business.entity.dto.QuoteSaveDTO;
import com.stonebridge.quotesystem.business.mapper.QuoteDetailMapper;
import com.stonebridge.quotesystem.business.mapper.QuoteMainMapper;
import com.stonebridge.quotesystem.business.mapper.QuoteSetItemMapper;
import com.stonebridge.quotesystem.business.service.IShapeSpecService;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuoteServiceImplTest {

    @Mock
    private QuoteMainMapper quoteMainMapper;
    @Mock
    private QuoteDetailMapper quoteDetailMapper;
    @Mock
    private QuoteSetItemMapper quoteSetItemMapper;
    @Mock
    private IShapeSpecService shapeSpecService;

    private QuoteServiceImpl quoteService;

    @BeforeEach
    void setUp() {
        quoteService = new QuoteServiceImpl(
                quoteMainMapper, quoteDetailMapper, quoteSetItemMapper, shapeSpecService);
        SysUser user = new SysUser();
        user.setId("fb20834256d57e6dbe659f7167f7ee12");
        user.setUsername("lin");
        user.setStatus(1);
        SecurityUser principal = new SecurityUser(user, List.of("salesman"), List.of("quote:create"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldSaveSetWithPerSetPricingAndUuidRelations() {
        when(quoteMainMapper.insert(org.mockito.ArgumentMatchers.any(QuoteMain.class))).thenReturn(1);
        QuoteSaveDTO dto = createBaseDto();

        QuoteDetail set = new QuoteDetail();
        set.setLineType("SET");
        set.setSortNo(2);
        set.setSetGroupId("SET_1753090000000_abc123");
        set.setSetName("套装 1");
        set.setCtns(10);
        set.setExtraPrice(new BigDecimal("1.50"));
        set.setCartonWeight(new BigDecimal("1.10"));
        set.setSetItems(List.of(
                setItem("FLMLPP100", "410", "6300", 2, 1),
                setItem("BOWL080", "300", "6000", 4, 2)));
        dto.setDetailList(List.of(set));

        quoteService.saveQuote(dto);

        ArgumentCaptor<QuoteMain> mainCaptor = ArgumentCaptor.forClass(QuoteMain.class);
        verify(quoteMainMapper).insert(mainCaptor.capture());
        QuoteMain main = mainCaptor.getValue();
        assertEquals(32, main.getId().length());
        assertEquals(main.getQuoteNo(), main.getOrderNo());
        assertEquals("20260721-客户A报价", main.getRemark());

        ArgumentCaptor<List<QuoteDetail>> detailCaptor = listCaptor();
        verify(quoteDetailMapper).insertBatch(detailCaptor.capture());
        QuoteDetail savedSet = detailCaptor.getValue().get(0);
        assertEquals("SET", savedSet.getLineType());
        assertEquals(main.getId(), savedSet.getOrderId());
        assertEquals(32, savedSet.getId().length());
        assertEquals(1, savedSet.getSetsPerCtn());
        assertEquals(6, savedSet.getPcsPerSet());
        assertEquals(6, savedSet.getPcs());
        assertEquals(60, savedSet.getTtlPcs());
        assertEquals(new BigDecimal("12.37"), savedSet.getSetUnitPrice());
        assertEquals(new BigDecimal("13.87"), savedSet.getUnitPrice());
        assertEquals(new BigDecimal("138.70"), savedSet.getAmount());
        assertNull(savedSet.getSpecCode());

        ArgumentCaptor<List<QuoteSetItem>> itemCaptor = listCaptor();
        verify(quoteSetItemMapper).insertBatch(itemCaptor.capture());
        List<QuoteSetItem> savedItems = itemCaptor.getValue();
        assertEquals(2, savedItems.size());
        assertEquals(new BigDecimal("2.583000"), savedItems.get(0).getComponentUnitPrice());
        assertEquals(new BigDecimal("1.800000"), savedItems.get(1).getComponentUnitPrice());
        assertTrue(savedItems.stream().allMatch(item -> item.getId() != null && item.getId().length() == 32));
        assertTrue(savedItems.stream().allMatch(item -> savedSet.getId().equals(item.getQuoteDetailId())));
    }

    @Test
    void shouldKeepSingleAmountFormulaAndNotCreateSetItems() {
        when(quoteMainMapper.insert(org.mockito.ArgumentMatchers.any(QuoteMain.class))).thenReturn(1);
        QuoteSaveDTO dto = createBaseDto();
        QuoteDetail single = new QuoteDetail();
        single.setLineType("SINGLE");
        single.setSpecCode("FLMLPP100");
        single.setPcsPerSet(6);
        single.setSetsPerCtn(8);
        single.setCtns(10);
        single.setTtlPcs(1);
        single.setUnitPrice(new BigDecimal("2.58"));
        single.setOriginalPrice(new BigDecimal("6300"));
        dto.setDetailList(List.of(single));

        quoteService.saveQuote(dto);

        ArgumentCaptor<List<QuoteDetail>> detailCaptor = listCaptor();
        verify(quoteDetailMapper).insertBatch(detailCaptor.capture());
        QuoteDetail savedSingle = detailCaptor.getValue().get(0);
        assertEquals(48, savedSingle.getPcs());
        assertEquals(480, savedSingle.getTtlPcs());
        assertEquals(new BigDecimal("1238.40"), savedSingle.getAmount());
        assertNotNull(savedSingle.getUnitPrice());
        verify(quoteSetItemMapper, never()).insertBatch(anyList());
    }

    @Test
    void shouldExpandSetIntoComponentRowsForExportWithoutRepeatingQuoteLineNumber() {
        QuoteDetail set = new QuoteDetail();
        set.setId("25567e6b023218ef6af5d70e331a9e02");
        set.setLineType("SET");
        set.setSetName("套装 1");
        set.setSetsPerCtn(1);
        set.setPcs(6);
        set.setTtlPcs(60);
        set.setCtns(10);
        set.setUnitPrice(new BigDecimal("13.87"));
        set.setSetItems(List.of(
                setItem("FLMLPP100", "410", "6300", 2, 1),
                setItem("BOWL080", "300", "6000", 4, 2)));

        List<QuoteExportDTO> rows = quoteService.buildExportList(List.of(set), Map.of());
        quoteService.assignExportIndexesAndGroups(rows);

        assertEquals(2, rows.size());
        assertTrue(rows.stream().allMatch(row -> Boolean.TRUE.equals(row.getSetLine())));
        assertTrue(rows.stream().allMatch(row -> set.getId().equals(row.getQuoteLineKey())));
        assertTrue(rows.stream().allMatch(row -> Integer.valueOf(1).equals(row.getItemIndex())));
        assertEquals("FLMLPP100", rows.get(0).getSpecCode());
        assertEquals(2, rows.get(0).getPcsPerSet());
        assertEquals("BOWL080", rows.get(1).getSpecCode());
        assertEquals(4, rows.get(1).getPcsPerSet());
        assertEquals(new BigDecimal("13.87"), rows.get(0).getUnitPrice());
        assertEquals(new BigDecimal("138.70"), rows.get(0).getAmount());
        assertEquals(rows.get(0).getGroupIndex(), rows.get(1).getGroupIndex());
    }

    private QuoteSaveDTO createBaseDto() {
        QuoteSaveDTO dto = new QuoteSaveDTO();
        dto.setCurrency("RMB");
        dto.setExchangeRate(new BigDecimal("6.8"));
        dto.setRemark("20260721-客户A报价");
        return dto;
    }

    private QuoteSetItem setItem(String specCode, String weight, String price,
                                 int qtyPerSet, int sortNo) {
        QuoteSetItem item = new QuoteSetItem();
        item.setSpecCode(specCode);
        item.setWeight(new BigDecimal(weight));
        item.setOriginalPrice(new BigDecimal(price));
        item.setQtyPerSet(qtyPerSet);
        item.setSortNo(sortNo);
        return item;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T> ArgumentCaptor<List<T>> listCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(List.class);
    }
}
