package com.stonebridge.quotesystem.business.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.metadata.data.DataFormatData;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteTableHolder;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.style.column.AbstractColumnWidthStyleStrategy;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stonebridge.quotesystem.business.entity.QuoteDetail;
import com.stonebridge.quotesystem.business.entity.QuoteMain;
import com.stonebridge.quotesystem.business.entity.QuoteSetItem;
import com.stonebridge.quotesystem.business.entity.ShapeSpec;
import com.stonebridge.quotesystem.business.entity.dto.QuoteExportDTO;
import com.stonebridge.quotesystem.business.entity.dto.QuoteSaveDTO;
import com.stonebridge.quotesystem.business.mapper.QuoteDetailMapper;
import com.stonebridge.quotesystem.business.mapper.QuoteMainMapper;
import com.stonebridge.quotesystem.business.mapper.QuoteSetItemMapper;
import com.stonebridge.quotesystem.business.service.IQuoteService;
import com.stonebridge.quotesystem.business.service.IShapeSpecService;
import com.stonebridge.quotesystem.business.strategy.QuoteSetMergeStrategy;
import com.stonebridge.quotesystem.business.strategy.ShapeImageMergeStrategy;
import com.stonebridge.quotesystem.exception.BusinessException;
import com.stonebridge.quotesystem.security.utils.SecurityUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.alibaba.excel.write.style.row.SimpleRowHeightStyleStrategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class QuoteServiceImpl implements IQuoteService {

    private static final Logger log = LoggerFactory.getLogger(QuoteServiceImpl.class);
    private static final String LINE_TYPE_SINGLE = "SINGLE";
    private static final String LINE_TYPE_SET = "SET";
    private static final BigDecimal ONE_MILLION = BigDecimal.valueOf(1_000_000L);
    private static final BigDecimal DEFAULT_CARTON_WEIGHT = new BigDecimal("1.1");
    private static final int MONEY_SCALE = 2;
    private static final int COMPONENT_PRICE_SCALE = 6;

    private final QuoteMainMapper quoteMainMapper;
    private final QuoteDetailMapper quoteDetailMapper;
    private final QuoteSetItemMapper quoteSetItemMapper;
    private final IShapeSpecService shapeSpecService;

    public QuoteServiceImpl(QuoteMainMapper quoteMainMapper,
                            QuoteDetailMapper quoteDetailMapper,
                            QuoteSetItemMapper quoteSetItemMapper,
                            IShapeSpecService shapeSpecService) {
        this.quoteMainMapper = quoteMainMapper;
        this.quoteDetailMapper = quoteDetailMapper;
        this.quoteSetItemMapper = quoteSetItemMapper;
        this.shapeSpecService = shapeSpecService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveQuote(QuoteSaveDTO dto) {
        validateQuote(dto);
        String quoteNo = normalizeQuoteNo(dto.getQuoteNo());
        String currentUserId = SecurityUtil.getCurrentUserId();
        QuoteMain main;
        if (hasQuoteNo(quoteNo)) {
            main = getQuoteMainForUpdate(quoteNo, currentUserId);
            updateQuoteMain(dto, main, currentUserId);
            deleteQuoteDetails(quoteNo);
        } else {
            main = createQuoteMain(dto, currentUserId);
            quoteNo = main.getQuoteNo();
        }

        saveQuoteDetails(main, dto);

        return quoteNo;
    }

    private void validateQuote(QuoteSaveDTO dto) {
        if (dto == null || dto.getDetailList() == null || dto.getDetailList().isEmpty()) {
            throw new BusinessException(400, "报价单明细不能为空");
        }
        if (dto.getExchangeRate() == null || dto.getExchangeRate().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "汇率必须大于0");
        }
    }

    private String normalizeQuoteNo(String quoteNo) {
        return quoteNo == null ? null : quoteNo.trim();
    }

    private boolean hasQuoteNo(String quoteNo) {
        return quoteNo != null && !quoteNo.trim().isEmpty();
    }

    private QuoteMain getQuoteMainForUpdate(String quoteNo, String currentUserId) {
        QuoteMain main = quoteMainMapper.selectOne(new QueryWrapper<QuoteMain>()
                .eq("quote_no", quoteNo)
                .and(w -> w.eq("creator", currentUserId).or().isNull("creator")));
        if (main == null) {
            throw new BusinessException(403, "报价单不存在或无权限修改");
        }
        return main;
    }

    private void updateQuoteMain(QuoteSaveDTO dto, QuoteMain main, String currentUserId) {
        UpdateWrapper<QuoteMain> mainUpdate = new UpdateWrapper<>();
        mainUpdate.eq("id", main.getId())
                // 数据隔离：只能更新自己创建的报价单；creator 为空仅用于兼容历史数据。
                .and(w -> w.eq("creator", currentUserId).or().isNull("creator"))
                .set("currency", defaultCurrency(dto.getCurrency()))
                .set("exchange_rate", dto.getExchangeRate())
                // 业务红线：整单 remark 始终写入主表，绝不下沉到明细表。
                .set("remark", dto.getRemark())
                .set("creator", currentUserId)
                .set("update_time", LocalDateTime.now());

        if (quoteMainMapper.update(null, mainUpdate) == 0) {
            throw new BusinessException(403, "报价单不存在或无权限修改");
        }
    }

    private QuoteMain createQuoteMain(QuoteSaveDTO dto, String currentUserId) {
        String quoteNo = getQuoteID();
        LocalDateTime now = LocalDateTime.now();
        QuoteMain main = new QuoteMain();
        main.setId(IdWorker.get32UUID());
        // 运行库原有 order_no 为非空列，新老单号保持同值，避免破坏旧表结构。
        main.setOrderNo(quoteNo);
        main.setQuoteNo(quoteNo);
        main.setCurrency(defaultCurrency(dto.getCurrency()));
        main.setExchangeRate(dto.getExchangeRate());
        // 业务红线：整单 remark 只保存在 QuoteMain。
        main.setRemark(dto.getRemark());
        main.setCreator(currentUserId);
        main.setCreateTime(now);
        main.setUpdateTime(now);
        quoteMainMapper.insert(main);
        return main;
    }

    private String defaultCurrency(String currency) {
        String normalized = currency == null ? "USD" : currency.trim().toUpperCase(Locale.ROOT);
        return "RMB".equals(normalized) ? "RMB" : "USD";
    }

    private void deleteQuoteDetails(String quoteNo) {
        quoteSetItemMapper.deleteByQuoteNo(quoteNo);
        quoteDetailMapper.delete(new QueryWrapper<QuoteDetail>().eq("quote_no", quoteNo));
    }

    private void saveQuoteDetails(QuoteMain main, QuoteSaveDTO dto) {
        PreparedQuoteData prepared = prepareQuoteDetails(main, dto);
        quoteDetailMapper.insertBatch(prepared.details());
        if (!prepared.setItems().isEmpty()) {
            quoteSetItemMapper.insertBatch(prepared.setItems());
        }
    }

    private PreparedQuoteData prepareQuoteDetails(QuoteMain main, QuoteSaveDTO dto) {
        Set<String> updatedSpecs = new HashSet<>();
        Set<Integer> usedSortNumbers = new HashSet<>();
        List<QuoteDetail> details = dto.getDetailList();
        List<QuoteDetail> preparedDetails = new ArrayList<>(details.size());
        List<QuoteSetItem> preparedSetItems = new ArrayList<>();
        for (int i = 0; i < details.size(); i++) {
            QuoteDetail detail = details.get(i);
            if (detail == null) {
                throw new BusinessException(400, "报价明细第" + (i + 1) + "行不能为空");
            }
            prepareCommonDetail(main, detail, i, usedSortNumbers);
            if (LINE_TYPE_SET.equals(detail.getLineType())) {
                prepareSetDetail(detail, dto, preparedSetItems, updatedSpecs);
            } else {
                prepareSingleDetail(detail, updatedSpecs);
            }
            preparedDetails.add(detail);
        }
        return new PreparedQuoteData(preparedDetails, preparedSetItems);
    }

    private void prepareCommonDetail(QuoteMain main, QuoteDetail detail, int index,
                                     Set<Integer> usedSortNumbers) {
        int sortNo = detail.getSortNo() != null && detail.getSortNo() > 0
                ? detail.getSortNo() : index + 1;
        if (!usedSortNumbers.add(sortNo)) {
            throw new BusinessException(400, "报价明细排序号不能重复：" + sortNo);
        }
        detail.setId(IdWorker.get32UUID());
        detail.setOrderId(main.getId());
        detail.setQuoteNo(main.getQuoteNo());
        detail.setItemIndex(sortNo);
        detail.setSortNo(sortNo);
        detail.setLineType(normalizeLineType(detail.getLineType()));
        detail.setCreateTime(LocalDateTime.now());
    }

    private String normalizeLineType(String lineType) {
        String normalized = lineType == null || lineType.trim().isEmpty()
                ? LINE_TYPE_SINGLE : lineType.trim().toUpperCase(Locale.ROOT);
        if (!LINE_TYPE_SINGLE.equals(normalized) && !LINE_TYPE_SET.equals(normalized)) {
            throw new BusinessException(400, "不支持的报价明细类型：" + lineType);
        }
        return normalized;
    }

    private void prepareSingleDetail(QuoteDetail detail, Set<String> updatedSpecs) {
        detail.setSetGroupId(null);
        detail.setSetName(null);
        detail.setSetUnitPrice(null);
        detail.setSetItems(null);

        if (detail.getOriginalPrice() == null) {
            detail.setOriginalPrice(detail.getPrice());
        }
        if (detail.getPcsPerSet() != null && detail.getSetsPerCtn() != null) {
            requirePositive(detail.getPcsPerSet(), "SINGLE的pcsPerSet必须大于0");
            requirePositive(detail.getSetsPerCtn(), "SINGLE的setsPerCtn必须大于0");
            detail.setPcs(multiplyExact(detail.getPcsPerSet(), detail.getSetsPerCtn(),
                    "SINGLE每箱件数超出整数范围"));
        }
        if (detail.getCtns() != null) {
            requirePositive(detail.getCtns(), "SINGLE的ctns必须大于0");
            if (detail.getPcs() != null) {
                detail.setTtlPcs(multiplyExact(detail.getPcs(), detail.getCtns(),
                        "SINGLE总件数超出整数范围"));
            }
        }
        detail.setAmount(calculateSingleAmount(detail.getTtlPcs(), detail.getUnitPrice()));
        updateOriginalPriceOnce(detail.getSpecCode(), detail.getOriginalPrice(), updatedSpecs);
    }

    private void prepareSetDetail(QuoteDetail detail, QuoteSaveDTO dto,
                                  List<QuoteSetItem> preparedSetItems,
                                  Set<String> updatedSpecs) {
        validateSetHeader(detail);
        BigDecimal exchangeDivisor = resolveExchangeDivisor(dto);
        Set<String> distinctProducts = new HashSet<>();
        Set<Integer> usedItemSortNumbers = new HashSet<>();
        BigDecimal rawSetPriceRmb = BigDecimal.ZERO;
        BigDecimal rawNetWeightGrams = BigDecimal.ZERO;
        int pcsPerSet = 0;

        List<QuoteSetItem> items = detail.getSetItems();
        for (int i = 0; i < items.size(); i++) {
            QuoteSetItem item = prepareSetItem(detail, items.get(i), i, usedItemSortNumbers);
            distinctProducts.add(item.getSpecCode());
            pcsPerSet = addExact(pcsPerSet, item.getQtyPerSet(), "SET每套总件数超出整数范围");

            BigDecimal componentPriceRmb = calculateComponentPriceRmb(item);
            rawSetPriceRmb = rawSetPriceRmb.add(
                    componentPriceRmb.multiply(BigDecimal.valueOf(item.getQtyPerSet())));
            rawNetWeightGrams = rawNetWeightGrams.add(
                    item.getWeight().multiply(BigDecimal.valueOf(item.getQtyPerSet())));

            item.setComponentUnitPrice(componentPriceRmb.divide(
                    exchangeDivisor, COMPONENT_PRICE_SCALE, RoundingMode.HALF_UP));
            preparedSetItems.add(item);
            updateOriginalPriceOnce(item.getSpecCode(), item.getOriginalPrice(), updatedSpecs);
        }
        if (distinctProducts.size() < 2) {
            throw new BusinessException(400, "SET至少需要两种不同产品");
        }

        applySetQuantities(detail, pcsPerSet);
        applySetWeights(detail, rawNetWeightGrams);
        applySetPrices(detail, rawSetPriceRmb, exchangeDivisor);
        clearSetParentProductFields(detail);
    }

    private void validateSetHeader(QuoteDetail detail) {
        if (detail.getSetGroupId() == null || detail.getSetGroupId().trim().isEmpty()) {
            throw new BusinessException(400, "SET的setGroupId不能为空");
        }
        if (detail.getCtns() == null) {
            throw new BusinessException(400, "SET的ctns不能为空");
        }
        requirePositive(detail.getCtns(), "SET的ctns必须大于0");
        if (detail.getSetItems() == null || detail.getSetItems().size() < 2) {
            throw new BusinessException(400, "SET至少需要两种产品");
        }
    }

    private QuoteSetItem prepareSetItem(QuoteDetail detail, QuoteSetItem item, int index,
                                        Set<Integer> usedSortNumbers) {
        if (item == null) {
            throw new BusinessException(400, "套装组件第" + (index + 1) + "项不能为空");
        }
        if (item.getSpecCode() == null || item.getSpecCode().trim().isEmpty()) {
            throw new BusinessException(400, "套装组件第" + (index + 1) + "项specCode不能为空");
        }
        item.setSpecCode(item.getSpecCode().trim());
        item.setOriginalPrice(item.getOriginalPrice() != null ? item.getOriginalPrice() : item.getPrice());
        requirePositive(item.getWeight(), "套装组件weight必须大于0：" + item.getSpecCode());
        requirePositive(item.getOriginalPrice(), "套装组件originalPrice必须大于0：" + item.getSpecCode());
        requirePositive(item.getQtyPerSet(), "套装组件qtyPerSet必须大于0：" + item.getSpecCode());

        int sortNo = item.getSortNo() != null && item.getSortNo() > 0 ? item.getSortNo() : index + 1;
        if (!usedSortNumbers.add(sortNo)) {
            throw new BusinessException(400, "套装组件排序号不能重复：" + sortNo);
        }
        item.setId(IdWorker.get32UUID());
        item.setQuoteDetailId(detail.getId());
        item.setSortNo(sortNo);
        item.setCreateTime(LocalDateTime.now());
        return item;
    }

    private BigDecimal calculateComponentPriceRmb(QuoteSetItem item) {
        // 除数是一百万（10的幂），因此这里可以精确除尽，不做任何中间舍入。
        return item.getWeight().multiply(item.getOriginalPrice()).divide(ONE_MILLION);
    }

    private BigDecimal resolveExchangeDivisor(QuoteSaveDTO dto) {
        return "USD".equals(defaultCurrency(dto.getCurrency()))
                ? dto.getExchangeRate() : BigDecimal.ONE;
    }

    private void applySetQuantities(QuoteDetail detail, int pcsPerSet) {
        detail.setSetsPerCtn(1);
        detail.setPcsPerSet(pcsPerSet);
        detail.setPcs(pcsPerSet);
        detail.setTtlPcs(multiplyExact(pcsPerSet, detail.getCtns(), "SET实物总件数超出整数范围"));
    }

    private void applySetWeights(QuoteDetail detail, BigDecimal rawNetWeightGrams) {
        BigDecimal netWeightKg = rawNetWeightGrams.divide(BigDecimal.valueOf(1000L));
        detail.setNwCtn(netWeightKg.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        BigDecimal cartonWeight = detail.getCartonWeight() == null
                ? DEFAULT_CARTON_WEIGHT : detail.getCartonWeight();
        detail.setCartonWeight(cartonWeight);
        detail.setGwCtn(netWeightKg.add(cartonWeight).setScale(MONEY_SCALE, RoundingMode.HALF_UP));
    }

    private void applySetPrices(QuoteDetail detail, BigDecimal rawSetPriceRmb,
                                BigDecimal exchangeDivisor) {
        BigDecimal extraPriceRmb = detail.getExtraPrice() == null ? BigDecimal.ZERO : detail.getExtraPrice();
        // 原始人民币价格完整累加后，仅在最终展示/入库价格处保留两位。
        detail.setSetUnitPrice(rawSetPriceRmb.divide(
                exchangeDivisor, MONEY_SCALE, RoundingMode.HALF_UP));
        detail.setUnitPrice(rawSetPriceRmb.add(extraPriceRmb).divide(
                exchangeDivisor, MONEY_SCALE, RoundingMode.HALF_UP));
        detail.setAmount(detail.getUnitPrice()
                .multiply(BigDecimal.valueOf(detail.getCtns().longValue()))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP));
    }

    private void clearSetParentProductFields(QuoteDetail detail) {
        detail.setSpecCode(null);
        detail.setDescription(null);
        detail.setDesign(null);
        detail.setWeight(null);
        detail.setOriginalPrice(null);
        detail.setPrice(null);
    }

    private void requirePositive(Integer value, String message) {
        if (value == null || value <= 0) {
            throw new BusinessException(400, message);
        }
    }

    private void requirePositive(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, message);
        }
    }

    private int multiplyExact(int left, int right, String message) {
        try {
            return Math.multiplyExact(left, right);
        } catch (ArithmeticException exception) {
            throw new BusinessException(400, message);
        }
    }

    private int addExact(int left, int right, String message) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException exception) {
            throw new BusinessException(400, message);
        }
    }

    private void updateOriginalPriceOnce(String specCode, BigDecimal originalPrice,
                                         Set<String> updatedSpecs) {
        if (originalPrice == null || specCode == null || !updatedSpecs.add(specCode)) {
            return;
        }
        shapeSpecService.update(new UpdateWrapper<ShapeSpec>()
                .eq("spec_code", specCode)
                .set("ton_price", originalPrice));
    }

    private record PreparedQuoteData(List<QuoteDetail> details, List<QuoteSetItem> setItems) {
    }

    public static String getQuoteID() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmmss"));
        String suffix = uuid.substring(uuid.length() - 6).toUpperCase();
        return timeStr + suffix;
    }

    @Override
    public void exportQuote(String quoteNo, List<String> columns, String headerLang, HttpServletResponse response) {
        try {
            QuoteMain main = getAccessibleQuoteMain(quoteNo, "报价单不存在或无权限导出");
            String currency = normalizeCurrency(main.getCurrency());
            String normalizedHeaderLang = normalizeHeaderLang(headerLang);

            List<QuoteDetail> detailList = loadQuoteDetails(quoteNo);
            Map<String, Map<String, Object>> shapeInfoMap = loadShapeInfoMap(detailList);
            List<QuoteExportDTO> exportList = buildExportList(detailList, shapeInfoMap);
            assignExportIndexesAndGroups(exportList);

            // 数据和权限校验完成后再设置文件响应，业务异常可以继续交给全局异常处理器返回标准JSON。
            configureExportResponse(response);
            setExportFileName(response, main.getRemark());
            writeQuoteWorkbook(response, currency, normalizedHeaderLang,
                    resolveExcludedFields(columns), exportList);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[exportQuote] 导出失败, quoteNo={}", quoteNo, e);
            resetResponseOnError(response, e);
        }
    }

    private void configureExportResponse(HttpServletResponse response) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
    }

    private QuoteMain getAccessibleQuoteMain(String quoteNo, String errorMessage) {
        QuoteMain main = quoteMainMapper.selectOne(
                new QueryWrapper<QuoteMain>().eq("quote_no", quoteNo));
        String currentUserId = SecurityUtil.getCurrentUserId();
        if (main == null || (main.getCreator() != null && !Objects.equals(main.getCreator(), currentUserId))) {
            throw new BusinessException(403, errorMessage);
        }
        return main;
    }

    private String normalizeCurrency(String currency) {
        String normalized = currency == null ? "USD" : currency.trim().toUpperCase(Locale.ROOT);
        return "RMB".equals(normalized) ? "RMB" : "USD";
    }

    private void setExportFileName(HttpServletResponse response, String mainRemark) {
        String remark = sanitizeFileNamePart(mainRemark);
        String rawFileName = "Quotation_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddHHmmss"))
                + "_" + randomFileSuffix();
        if (!remark.isEmpty()) {
            rawFileName += "_" + remark;
        }
        String fileName = java.net.URLEncoder.encode(rawFileName, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
    }

    private String sanitizeFileNamePart(String value) {
        return value == null ? "" : value.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String randomFileSuffix() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder result = new StringBuilder(5);
        java.util.concurrent.ThreadLocalRandom random = java.util.concurrent.ThreadLocalRandom.current();
        for (int i = 0; i < 5; i++) {
            result.append(chars.charAt(random.nextInt(chars.length())));
        }
        return result.toString();
    }

    private List<QuoteDetail> loadQuoteDetails(String quoteNo) {
        List<QuoteDetail> details = quoteDetailMapper.selectList(
                new QueryWrapper<QuoteDetail>()
                        .eq("quote_no", quoteNo)
                        .orderByAsc("item_index"));
        attachSetItems(details);
        populateDescriptions(details);
        return details;
    }

    private void attachSetItems(List<QuoteDetail> details) {
        List<String> setDetailIds = details.stream()
                .filter(detail -> LINE_TYPE_SET.equals(detail.getLineType()))
                .map(QuoteDetail::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (setDetailIds.isEmpty()) {
            return;
        }

        List<QuoteSetItem> items = quoteSetItemMapper.selectList(
                new QueryWrapper<QuoteSetItem>()
                        .in("quote_detail_id", setDetailIds)
                        .orderByAsc("quote_detail_id", "sort_no"));
        Map<String, List<QuoteSetItem>> itemsByDetailId = items.stream()
                .collect(Collectors.groupingBy(QuoteSetItem::getQuoteDetailId,
                        LinkedHashMap::new, Collectors.toList()));
        for (QuoteDetail detail : details) {
            if (LINE_TYPE_SET.equals(detail.getLineType())) {
                detail.setSetItems(itemsByDetailId.getOrDefault(detail.getId(), Collections.emptyList()));
            }
        }
    }

    private Map<String, Map<String, Object>> loadShapeInfoMap(List<QuoteDetail> details) {
        List<String> detailSpecCodes = details.stream()
                .map(QuoteDetail::getSpecCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<String> setItemSpecCodes = details.stream()
                .map(QuoteDetail::getSetItems)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .map(QuoteSetItem::getSpecCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<String> specCodes = new ArrayList<>(detailSpecCodes.size() + setItemSpecCodes.size());
        specCodes.addAll(detailSpecCodes);
        specCodes.addAll(setItemSpecCodes);
        specCodes = specCodes.stream().distinct().collect(Collectors.toList());
        if (specCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        return quoteDetailMapper.findShapeAndImageByCodes(specCodes).stream()
                .filter(item -> item.get("specCode") != null)
                .collect(Collectors.toMap(
                        item -> String.valueOf(item.get("specCode")),
                        item -> item,
                        (left, right) -> left));
    }

    List<QuoteExportDTO> buildExportList(List<QuoteDetail> details,
                                         Map<String, Map<String, Object>> shapeInfoMap) {
        List<QuoteExportDTO> exportList = new ArrayList<>();
        for (int i = 0; i < details.size(); i++) {
            QuoteDetail detail = details.get(i);
            String quoteLineKey = detail.getId() != null ? detail.getId() : "QUOTE_LINE_" + i;
            if (LINE_TYPE_SET.equals(detail.getLineType())) {
                exportList.addAll(buildSetExportRows(detail, quoteLineKey, shapeInfoMap));
            } else {
                exportList.add(buildSingleExportRow(detail, quoteLineKey, shapeInfoMap));
            }
        }
        return exportList;
    }

    void assignExportIndexesAndGroups(List<QuoteExportDTO> exportList) {
        String currentQuoteLine = null;
        String currentStyleGroup = null;
        int quoteLineIndex = 0;
        int groupIndex = -1;
        for (QuoteExportDTO dto : exportList) {
            if (!Objects.equals(dto.getQuoteLineKey(), currentQuoteLine)) {
                currentQuoteLine = dto.getQuoteLineKey();
                quoteLineIndex++;
            }
            dto.setItemIndex(quoteLineIndex);

            String styleGroup = Boolean.TRUE.equals(dto.getSetLine())
                    ? "SET:" + dto.getQuoteLineKey()
                    : "SHAPE:" + dto.getShapeCode();
            if (!Objects.equals(styleGroup, currentStyleGroup)) {
                currentStyleGroup = styleGroup;
                groupIndex++;
            }
            dto.setGroupIndex(groupIndex);
        }
    }

    private Set<String> resolveExcludedFields(List<String> columns) {
        Set<String> excluded = new HashSet<>();
        List<String> selected = columns == null ? Collections.emptyList() : columns;
        if (!selected.contains("weight")) excluded.add("weight");
        if (!selected.contains("dimension")) excluded.add("dimension");
        if (!selected.contains("price")) excluded.add("price");
        if (!selected.contains("extraPrice")) excluded.add("extraPrice");
        if (!selected.contains("cartonWeight")) excluded.add("cartonWeight");
        if (!selected.contains("remarks")) excluded.add("remarks");
        return excluded;
    }

    private void writeQuoteWorkbook(HttpServletResponse response, String currency, String headerLang,
                                    Set<String> excludedFields, List<QuoteExportDTO> exportList) throws Exception {
        WriteCellStyle headStyle = createHeadStyle();
        WriteCellStyle contentStyle = createContentStyle();
        EasyExcel.write(response.getOutputStream(), QuoteExportDTO.class)
                .inMemory(true)
                .excludeColumnFieldNames(excludedFields)
                .registerWriteHandler(new HorizontalCellStyleStrategy(headStyle, contentStyle))
                .registerWriteHandler(new SimpleRowHeightStyleStrategy((short) 30, (short) 60))
                .registerWriteHandler(buildColumnWidthStrategy())
                .registerWriteHandler(new QuoteHeaderLanguageCellWriteHandler(headerLang))
                .registerWriteHandler(new QuoteDynamicStyleCellWriteHandler(currency, exportList))
                .registerWriteHandler(new QuoteSetMergeStrategy(exportList))
                .registerWriteHandler(new ShapeImageMergeStrategy(exportList))
                .sheet("Quote Data")
                .doWrite(exportList);
    }

    private WriteCellStyle createHeadStyle() {
        WriteCellStyle style = new WriteCellStyle();
        style.setFillPatternType(FillPatternType.SOLID_FOREGROUND);
        style.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
        WriteFont font = new WriteFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints((short) 11);
        font.setBold(true);
        style.setWriteFont(font);
        style.setHorizontalAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setCellBorder(style);
        return style;
    }

    private WriteCellStyle createContentStyle() {
        WriteCellStyle style = new WriteCellStyle();
        WriteFont font = new WriteFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints((short) 10);
        style.setWriteFont(font);
        style.setHorizontalAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapped(Boolean.TRUE);
        setCellBorder(style);
        return style;
    }
    private String normalizeHeaderLang(String headerLang) {
        if (headerLang == null || headerLang.trim().isEmpty()) {
            return "EN";
        }

        String value = headerLang.trim().toUpperCase(Locale.ROOT);

        if ("CN".equals(value) || "ZH".equals(value) || "CHINESE".equals(value)) {
            return "CN";
        }

        return "EN";
    }

    /**
     * Excel 表头语言动态切换处理器。
     *
     * EN：英文表头
     * CN：中文表头
     *
     * 说明：
     * 1. 不需要复制两个 QuoteExportDTO；
     * 2. 继续使用 QuoteExportDTO 的字段顺序和二级表头结构；
     * 3. 只在写表头单元格时替换显示文字；
     * 4. 不影响金额格式、动态列、图片合并、斑马纹。
     */
    private static class QuoteHeaderLanguageCellWriteHandler implements CellWriteHandler {

        private final String headerLang;
        private final boolean chinese;

        public QuoteHeaderLanguageCellWriteHandler(String headerLang) {
            this.headerLang = headerLang == null ? "EN" : headerLang.trim().toUpperCase(Locale.ROOT);
            this.chinese = "CN".equals(this.headerLang);
        }

        @Override
        public void afterCellDispose(WriteSheetHolder writeSheetHolder,
                                     WriteTableHolder writeTableHolder,
                                     List<WriteCellData<?>> cellDataList,
                                     Cell cell,
                                     Head head,
                                     Integer relativeRowIndex,
                                     Boolean isHead) {
            if (!Boolean.TRUE.equals(isHead) || cell == null || head == null) {
                return;
            }

            String fieldName = head.getFieldName();
            if (fieldName == null || fieldName.trim().isEmpty()) {
                return;
            }

            String text = getHeaderText(fieldName, cell.getRowIndex());
            if (text == null) {
                return;
            }

            cell.setCellValue(text);
            adjustColumnWidth(cell, text);
        }

        private String getHeaderText(String fieldName, int rowIndex) {
            if ("cbmCtn".equals(fieldName)) {
                return twoLevel(rowIndex, chinese ? "体积(m³)" : "CBM", chinese ? "箱" : "ctn");
            }

            if ("cbmTotal".equals(fieldName)) {
                return twoLevel(rowIndex, chinese ? "体积(m³)" : "CBM", chinese ? "合计" : "total");
            }

            if ("nwCtn".equals(fieldName)) {
                return twoLevel(rowIndex, chinese ? "净重(kg)" : "N.W.(kg)", chinese ? "箱" : "ctn");
            }

            if ("nwTotal".equals(fieldName)) {
                return twoLevel(rowIndex, chinese ? "净重(kg)" : "N.W.(kg)", chinese ? "合计" : "total");
            }

            if ("gwCtn".equals(fieldName)) {
                return twoLevel(rowIndex, chinese ? "毛重(kg)" : "G.W.(kg)", chinese ? "箱" : "ctn");
            }

            if ("gwTotal".equals(fieldName)) {
                return twoLevel(rowIndex, chinese ? "毛重(kg)" : "G.W.(kg)", chinese ? "合计" : "total");
            }

            if (chinese) {
                return getChineseSingleHeader(fieldName);
            }

            return getEnglishSingleHeader(fieldName);
        }

        private String twoLevel(int rowIndex, String firstLevel, String secondLevel) {
            return rowIndex <= 0 ? firstLevel : secondLevel;
        }

        private String getEnglishSingleHeader(String fieldName) {
            Map<String, String> map = new HashMap<>();

            map.put("itemIndex", "NO.");
            map.put("specCode", "ITEM NO.");
            map.put("description", "DESCRIPTION");
            map.put("photoPlaceholder", "Product photo");
            map.put("design", "DESIGN");

            map.put("pcsPerSet", "PCS/SET");
            map.put("setsPerCtn", "SETS/CTN");
            map.put("pcs", "PCS");
            map.put("ttlPcs", "TTL PCS");
            map.put("ctns", "TTL CTNs");

            map.put("unitPrice", "U.PRICE");
            map.put("amount", "AMOUNT");

            map.put("weight", "Weight(g)");
            map.put("dimension", "Volume(cm)");
            map.put("price", "Price/ton");
            map.put("extraPrice", "Extra Price");
            map.put("cartonWeight", "Carton(kg)");
            map.put("remarks", "Remarks");

            return map.get(fieldName);
        }

        private String getChineseSingleHeader(String fieldName) {
            Map<String, String> map = new HashMap<>();

            map.put("itemIndex", "序号");
            map.put("specCode", "产品编号");
            map.put("description", "产品描述");
            map.put("photoPlaceholder", "产品图片");
            map.put("design", "图案/设计");

            map.put("pcsPerSet", "每套件数");
            map.put("setsPerCtn", "每箱套数");
            map.put("pcs", "每箱件数");
            map.put("ttlPcs", "总件数");
            map.put("ctns", "总箱数");

            map.put("unitPrice", "单价");
            map.put("amount", "金额");

            map.put("weight", "单重(g)");
            map.put("dimension", "尺寸(cm)");
            map.put("price", "吨价");
            map.put("extraPrice", "额外价格");
            map.put("cartonWeight", "纸箱重量(kg)");
            map.put("remarks", "备注信息");

            return map.get(fieldName);
        }

        private void adjustColumnWidth(Cell cell, String value) {
            if (value == null || value.isEmpty()) {
                return;
            }

            int displayLength = 0;
            for (char c : value.toCharArray()) {
                displayLength += c > 127 ? 2 : 1;
            }

            int currentWidth = cell.getSheet().getColumnWidth(cell.getColumnIndex()) / 256;
            int targetWidth = Math.min(Math.max(currentWidth, displayLength + 4), 60);

            cell.getSheet().setColumnWidth(cell.getColumnIndex(), targetWidth * 256);
        }
    }

    /**
     * 【增强优化】提取器型英文字母前缀（兼容数字开头的型号，如 1LMSPDP80 -> 1LMSPDP）
     */
    private static String extractShapePrefix(String specCode) {
        if (specCode == null) return "";
        // ★ 核心修复2：允许前缀包含开头数字
        Matcher m = Pattern.compile("^([0-9]*[a-zA-Z]+)").matcher(specCode);
        if (m.find()) {
            return m.group(1);
        }
        return specCode;
    }

    /**
     * 【增强优化】提取器型尺寸数字
     */
    private static int extractSizeNumber(String specCode) {
        if (specCode == null) return 0;
        // ★ 核心修复2：允许前缀包含开头数字
        Matcher m = Pattern.compile("^([0-9]*[a-zA-Z]+)(\\d*)").matcher(specCode);
        if (m.find()) {
            String numStr = m.group(2);
            return (numStr != null && !numStr.isEmpty()) ? Integer.parseInt(numStr) : 0;
        }
        return 0;
    }

    /**
     * 动态样式拦截器：精准按 FieldName 赋予货币格式，重塑斑马纹并保留网格线边框
     */
    private static class QuoteDynamicStyleCellWriteHandler implements CellWriteHandler {
        private final String currency;
        private final List<QuoteExportDTO> exportList;

        public QuoteDynamicStyleCellWriteHandler(String currency, List<QuoteExportDTO> exportList) {
            this.currency = currency;
            this.exportList = exportList;
        }

        @Override
        public void afterCellDataConverted(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder,
                                           WriteCellData<?> cellData, Cell cell, Head head, Integer relativeRowIndex, Boolean isHead) {
            if (Boolean.TRUE.equals(isHead) || cellData == null || relativeRowIndex == null) {
                return;
            }

            WriteCellStyle writeCellStyle = cellData.getOrCreateStyle();

            // 1. 设置原生货币数字格式
            if (head != null && ("unitPrice".equals(head.getFieldName()) || "amount".equals(head.getFieldName()))) {
                DataFormatData dataFormatData = writeCellStyle.getDataFormatData();
                if (dataFormatData == null) {
                    dataFormatData = new DataFormatData();
                    writeCellStyle.setDataFormatData(dataFormatData);
                }
                String formatStr = "RMB".equals(currency) ? "[$¥-804]#,##0.00" : "[$$-409]#,##0.00";
                dataFormatData.setFormat(formatStr);
            }

            // 2. 根据 DTO 里的 groupIndex 动态绘制底色
            QuoteExportDTO dto = exportList.get(relativeRowIndex);
            if (dto != null && dto.getGroupIndex() != null) {
                if (dto.getGroupIndex() % 2 == 0) {
                    writeCellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                } else {
                    writeCellStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
                }
                writeCellStyle.setFillPatternType(FillPatternType.SOLID_FOREGROUND);

                // ★ 修复：重新赋予边框，防止纯色背景遮挡原生网格线
                writeCellStyle.setBorderLeft(BorderStyle.THIN);
                writeCellStyle.setBorderRight(BorderStyle.THIN);
                writeCellStyle.setBorderTop(BorderStyle.THIN);
                writeCellStyle.setBorderBottom(BorderStyle.THIN);
                writeCellStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
                writeCellStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());
                writeCellStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
                writeCellStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());

                writeCellStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
                writeCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
                writeCellStyle.setWrapped(Boolean.TRUE);
            }
        }
    }

    private AbstractColumnWidthStyleStrategy buildColumnWidthStrategy() {
        return new AbstractColumnWidthStyleStrategy() {
            private final Map<Integer, Integer> columnWidthMap = new HashMap<>();

            @Override
            protected void setColumnWidth(WriteSheetHolder writeSheetHolder,
                                          List<WriteCellData<?>> cellDataList,
                                          Cell cell,
                                          Head head,
                                          Integer relativeRowIndex,
                                          Boolean isHead) {
                if (cellDataList == null || cellDataList.isEmpty()) return;
                if (head != null && (head.getHeadNameList() == null
                        || head.getHeadNameList().stream().allMatch(s -> s == null || s.isEmpty()))) return;

                int columnIndex = cell.getColumnIndex();
                int dataLength = 0;

                if (Boolean.TRUE.equals(isHead)) {
                    dataLength = head.getHeadNameList().stream()
                            .mapToInt(s -> s == null ? 0 : s.getBytes(StandardCharsets.UTF_8).length)
                            .max().orElse(0);
                } else {
                    WriteCellData<?> cellData = cellDataList.get(0);
                    if (cellData != null) {
                        if (cellData.getStringValue() != null) {
                            dataLength = cellData.getStringValue().chars()
                                    .map(c -> c > 127 ? 2 : 1)
                                    .sum();
                        } else if (cellData.getNumberValue() != null) {
                            dataLength = cellData.getNumberValue().toPlainString().length();
                        }
                    }
                }

                if (dataLength == 0) return;

                int maxWidth = Math.min(Math.max(
                        columnWidthMap.getOrDefault(columnIndex, 10),
                        dataLength + 2
                ), 60);

                columnWidthMap.put(columnIndex, maxWidth);
                writeSheetHolder.getSheet().setColumnWidth(columnIndex, maxWidth * 256);
            }
        };
    }

    private void resetResponseOnError(HttpServletResponse response, Exception e) {
        try {
            if (!response.isCommitted()) {
                response.reset();
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().write("{\"code\":500,\"msg\":\"导出失败，请稍后重试\"}");
            }
        } catch (Exception ex) {
            log.error("[exportQuote] 重置响应失败", ex);
        }
    }

    private void setCellBorder(WriteCellStyle cellStyle) {
        cellStyle.setBorderLeft(BorderStyle.THIN);
        cellStyle.setBorderRight(BorderStyle.THIN);
        cellStyle.setBorderTop(BorderStyle.THIN);
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        cellStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());
        cellStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
        cellStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
    }

    private QuoteExportDTO buildSingleExportRow(QuoteDetail detail, String quoteLineKey,
                                                Map<String, Map<String, Object>> shapeInfoMap) {
        QuoteExportDTO dto = new QuoteExportDTO();
        copyExportFields(detail, dto);
        dto.setQuoteLineKey(quoteLineKey);
        dto.setSetLine(false);
        applyShapeInfo(detail.getSpecCode(), dto, shapeInfoMap);
        applyExportCalculations(detail, dto);
        return dto;
    }

    private List<QuoteExportDTO> buildSetExportRows(QuoteDetail detail, String quoteLineKey,
                                                    Map<String, Map<String, Object>> shapeInfoMap) {
        if (detail.getSetItems() == null || detail.getSetItems().isEmpty()) {
            throw new BusinessException(500, "套装报价缺少组件数据，无法导出：" + detail.getSetName());
        }

        List<QuoteExportDTO> rows = new ArrayList<>(detail.getSetItems().size());
        for (QuoteSetItem item : detail.getSetItems()) {
            QuoteExportDTO dto = new QuoteExportDTO();
            copyExportFields(detail, dto);
            copySetItemExportFields(item, dto);
            dto.setQuoteLineKey(quoteLineKey);
            dto.setSetLine(true);
            applyShapeInfo(item.getSpecCode(), dto, shapeInfoMap);
            applyExportCalculations(detail, dto);
            rows.add(dto);
        }
        return rows;
    }

    private void copySetItemExportFields(QuoteSetItem item, QuoteExportDTO dto) {
        dto.setSpecCode(item.getSpecCode());
        dto.setDescription(item.getDescription());
        dto.setDesign(item.getDesign());
        dto.setPcsPerSet(item.getQtyPerSet());
        dto.setWeight(item.getWeight());
        dto.setPrice(item.getOriginalPrice());
    }

    private void copyExportFields(QuoteDetail detail, QuoteExportDTO dto) {
        dto.setSpecCode(detail.getSpecCode());
        dto.setDescription(detail.getDescription());
        dto.setDesign(detail.getDesign());
        dto.setPcsPerSet(detail.getPcsPerSet());
        dto.setSetsPerCtn(detail.getSetsPerCtn());
        dto.setPcs(detail.getPcs());
        dto.setCbmCtn(detail.getCbmCtn());
        dto.setGwCtn(detail.getGwCtn());
        dto.setNwCtn(detail.getNwCtn());
        dto.setTtlPcs(detail.getTtlPcs());

        dto.setWeight(detail.getWeight());
        dto.setDimension(detail.getDimension());
        dto.setPrice(detail.getOriginalPrice());
        dto.setExtraPrice(detail.getExtraPrice());
        dto.setCartonWeight(detail.getCartonWeight());
        dto.setRemarks(detail.getRemarks());
        dto.setUnitPrice(detail.getUnitPrice());
    }

    private void applyShapeInfo(String specCode, QuoteExportDTO dto,
                                Map<String, Map<String, Object>> shapeInfoMap) {
        Map<String, Object> shapeInfo = specCode != null ? shapeInfoMap.get(specCode) : null;
        if (shapeInfo != null && shapeInfo.get("shapeCode") != null) {
            dto.setShapeCode(String.valueOf(shapeInfo.get("shapeCode")));
            applyShapeImage(specCode, dto, shapeInfo.get("imageData"));
        } else {
            dto.setShapeCode(extractShapePrefix(specCode));
        }
    }

    private void applyShapeImage(String specCode, QuoteExportDTO dto, Object imageData) {
        if (imageData == null) {
            return;
        }
        try {
            if (imageData instanceof byte[] bytes && bytes.length > 0) {
                dto.setPhoto(bytes);
            } else if (imageData instanceof java.sql.Blob blob) {
                dto.setPhoto(blob.getBytes(1, (int) blob.length()));
            }
        } catch (Exception e) {
            log.warn("[buildExportDTO] 解析图片失败，specCode={}", specCode, e);
        }
    }

    private void applyExportCalculations(QuoteDetail detail, QuoteExportDTO dto) {
        Integer ttlPcs = resolveTtlPcs(detail);
        dto.setTtlPcs(ttlPcs);
        if (detail.getCtns() != null && detail.getCtns() > 0) {
            BigDecimal ctnsDec = BigDecimal.valueOf(detail.getCtns());
            dto.setCtns(detail.getCtns());
            if (detail.getCbmCtn() != null) dto.setCbmTotal(detail.getCbmCtn().multiply(ctnsDec));
            if (detail.getGwCtn() != null) dto.setGwTotal(detail.getGwCtn().multiply(ctnsDec));
            if (detail.getNwCtn() != null) dto.setNwTotal(detail.getNwCtn().multiply(ctnsDec));
        }
        dto.setAmount(calculateAmount(detail));
    }

    @Override
    public Page<QuoteMain> getHistoryPage(Integer current, Integer size, String quoteNo, String remarks) {
        Page<QuoteMain> page = new Page<>(current, size);
        QueryWrapper<QuoteMain> wrapper = new QueryWrapper<>();

        // ★ 新增：数据隔离，只能查询当前登录人创建的数据
        String currentUserId = SecurityUtil.getCurrentUserId();
        // 使用 and(w -> ...) 嵌套，等价于 SQL 中的：AND (creator = '当前用户' OR creator IS NULL)
        wrapper.and(w -> w.eq("creator", currentUserId).or().isNull("creator"));
        if (quoteNo != null && !quoteNo.trim().isEmpty()) {
            wrapper.like("quote_no", quoteNo.trim().toUpperCase());
        }

        if (remarks != null && !remarks.trim().isEmpty()) {
            wrapper.like("remark", remarks.trim());
        }

        wrapper.orderByDesc("update_time", "create_time");
        return quoteMainMapper.selectPage(page, wrapper);
    }

    @Override
    public List<QuoteDetail> getDetailsByQuoteNo(String quoteNo) {
        if (quoteNo == null || quoteNo.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String normalizedQuoteNo = quoteNo.trim();
        String currentUserId = SecurityUtil.getCurrentUserId();
        QuoteMain main = quoteMainMapper.selectOne(
                new QueryWrapper<QuoteMain>().eq("quote_no", normalizedQuoteNo));
        if (main == null || (main.getCreator() != null && !Objects.equals(main.getCreator(), currentUserId))) {
            throw new BusinessException(403, "报价单不存在或无权限访问");
        }

        List<QuoteDetail> list = loadQuoteDetails(normalizedQuoteNo);

        String currency = main.getCurrency() != null ? main.getCurrency() : "USD";

        for (QuoteDetail detail : list) {
            applyAmount(detail);
            applyDerivedTotals(detail);
            detail.setSortNo(detail.getItemIndex());
            detail.setCurrency(currency);
        }

        return list;
    }

    private void applyAmount(QuoteDetail detail) {
        Integer ttlPcs = resolveTtlPcs(detail);
        if (detail.getTtlPcs() == null) {
            detail.setTtlPcs(ttlPcs);
        }
        detail.setAmount(calculateAmount(detail));
    }

    private void applyDerivedTotals(QuoteDetail detail) {
        if (detail.getCtns() == null || detail.getCtns() <= 0) {
            return;
        }
        BigDecimal ctns = BigDecimal.valueOf(detail.getCtns().longValue());
        if (detail.getCbmCtn() != null) {
            detail.setCbmTotal(detail.getCbmCtn().multiply(ctns));
        }
        if (detail.getNwCtn() != null) {
            detail.setNwTotal(detail.getNwCtn().multiply(ctns));
        }
        if (detail.getGwCtn() != null) {
            detail.setGwTotal(detail.getGwCtn().multiply(ctns));
        }
    }

    private Integer resolveTtlPcs(QuoteDetail detail) {
        if (detail.getTtlPcs() != null) {
            return detail.getTtlPcs();
        }
        if (detail.getPcs() == null || detail.getCtns() == null || detail.getCtns() <= 0) {
            return null;
        }
        // 发现整数溢出时直接抛出异常，禁止产生错误的总件数和金额。
        return Math.multiplyExact(detail.getPcs(), detail.getCtns());
    }

    private BigDecimal calculateAmount(QuoteDetail detail) {
        if (detail.getUnitPrice() == null) {
            return null;
        }
        if (LINE_TYPE_SET.equals(detail.getLineType())) {
            if (detail.getCtns() == null) {
                return null;
            }
            // SET：U.PRICE 的单位是元/套，且 SETS/CTN 固定为1，因此按箱数（套数）计价。
            return detail.getUnitPrice()
                    .multiply(BigDecimal.valueOf(detail.getCtns().longValue()))
                    .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        return calculateSingleAmount(resolveTtlPcs(detail), detail.getUnitPrice());
    }

    private BigDecimal calculateSingleAmount(Integer ttlPcs, BigDecimal unitPrice) {
        if (ttlPcs == null || unitPrice == null) {
            return null;
        }
        // SINGLE核心公式：TTL PCS × U.PRICE = AMOUNT；全程使用BigDecimal。
        return unitPrice.multiply(BigDecimal.valueOf(ttlPcs.longValue()));
    }

    private void populateDescriptions(List<QuoteDetail> list) {
        if (list == null || list.isEmpty()) return;

        List<String> detailSpecCodes = list.stream()
                .map(QuoteDetail::getSpecCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<String> itemSpecCodes = list.stream()
                .map(QuoteDetail::getSetItems)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .map(QuoteSetItem::getSpecCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<String> specCodes = new ArrayList<>(detailSpecCodes.size() + itemSpecCodes.size());
        specCodes.addAll(detailSpecCodes);
        specCodes.addAll(itemSpecCodes);
        specCodes = specCodes.stream().distinct().collect(Collectors.toList());

        if (!specCodes.isEmpty()) {
            List<ShapeSpec> specList = shapeSpecService.list(
                    new QueryWrapper<ShapeSpec>()
                            .select("spec_code", "description")
                            .in("spec_code", specCodes)
            );

            Map<String, String> descMap = specList.stream()
                    .filter(s -> s.getSpecCode() != null && s.getDescription() != null)
                    .collect(Collectors.toMap(ShapeSpec::getSpecCode, ShapeSpec::getDescription, (a, b) -> a));

            for (QuoteDetail detail : list) {
                if (detail.getSpecCode() != null && descMap.containsKey(detail.getSpecCode())) {
                    detail.setDescription(descMap.get(detail.getSpecCode()));
                }
                if (detail.getSetItems() != null) {
                    for (QuoteSetItem item : detail.getSetItems()) {
                        item.setDescription(descMap.get(item.getSpecCode()));
                        item.setPrice(item.getOriginalPrice());
                    }
                }
            }
        }
    }
}
