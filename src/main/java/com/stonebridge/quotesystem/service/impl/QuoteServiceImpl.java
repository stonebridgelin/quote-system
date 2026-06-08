package com.stonebridge.quotesystem.service.impl;

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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stonebridge.quotesystem.entity.QuoteDetail;
import com.stonebridge.quotesystem.entity.QuoteMain;
import com.stonebridge.quotesystem.entity.ShapeSpec;
import com.stonebridge.quotesystem.entity.dto.QuoteExportDTO;
import com.stonebridge.quotesystem.entity.dto.QuoteSaveDTO;
import com.stonebridge.quotesystem.mapper.QuoteDetailMapper;
import com.stonebridge.quotesystem.mapper.QuoteMainMapper;
import com.stonebridge.quotesystem.service.IQuoteService;
import com.stonebridge.quotesystem.service.IShapeSpecService;
import com.stonebridge.quotesystem.strategy.ShapeImageMergeStrategy;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.alibaba.excel.write.style.row.SimpleRowHeightStyleStrategy;

import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 报价单 Service 实现
 */
@Service
public class QuoteServiceImpl implements IQuoteService {

    private static final Logger log = LoggerFactory.getLogger(QuoteServiceImpl.class);

    private final QuoteMainMapper quoteMainMapper;
    private final QuoteDetailMapper quoteDetailMapper;
    private final IShapeSpecService shapeSpecService;

    public QuoteServiceImpl(QuoteMainMapper quoteMainMapper,
                            QuoteDetailMapper quoteDetailMapper,
                            IShapeSpecService shapeSpecService) {
        this.quoteMainMapper = quoteMainMapper;
        this.quoteDetailMapper = quoteDetailMapper;
        this.shapeSpecService = shapeSpecService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveQuote(QuoteSaveDTO dto) {
        String quoteNo = dto.getQuoteNo();
        boolean isUpdate = (quoteNo != null && !quoteNo.trim().isEmpty());

        if (isUpdate) {
            // 【1. 更新操作】：只把 remark 等元数据更新到 t_quote_main
            UpdateWrapper<QuoteMain> mainUpdate = new UpdateWrapper<>();
            mainUpdate.eq("quote_no", quoteNo)
                    .set("currency", dto.getCurrency() != null ? dto.getCurrency() : "USD")
                    .set("exchange_rate", dto.getExchangeRate())
                    .set("remark", dto.getRemark()); // ★ 将备注更新到主表
            quoteMainMapper.update(null, mainUpdate);

            quoteDetailMapper.delete(new QueryWrapper<QuoteDetail>().eq("quote_no", quoteNo));
        } else {
            // 【2. 新增操作】：新建单号，并将 remark 保存入 t_quote_main
            quoteNo = getQuoteID();
            QuoteMain main = new QuoteMain();
            main.setQuoteNo(quoteNo);
            main.setCurrency(dto.getCurrency() != null ? dto.getCurrency() : "USD");
            main.setExchangeRate(dto.getExchangeRate());
            main.setRemark(dto.getRemark()); // ★ 将备注保存到主表
            quoteMainMapper.insert(main);
        }

        List<QuoteDetail> details = dto.getDetailList();
        if (details != null && !details.isEmpty()) {
            Set<String> updatedSpecs = new HashSet<>();
            List<QuoteDetail> toInsert = new ArrayList<>(details.size());

            for (int i = 0; i < details.size(); i++) {
                QuoteDetail detail = details.get(i);
                detail.setId(null);
                detail.setQuoteNo(quoteNo);
                detail.setItemIndex(i + 1);

                if (detail.getOriginalPrice() != null && detail.getSpecCode() != null
                        && !updatedSpecs.contains(detail.getSpecCode())) {
                    shapeSpecService.update(new UpdateWrapper<ShapeSpec>()
                            .eq("spec_code", detail.getSpecCode())
                            .set("ton_price", detail.getOriginalPrice()));
                    updatedSpecs.add(detail.getSpecCode());
                }

                toInsert.add(detail);
            }
            quoteDetailMapper.insertBatch(toInsert);
        }

        return quoteNo;
    }

    public static String getQuoteID() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmmss"));
        String suffix = uuid.substring(uuid.length() - 6).toUpperCase();
        return timeStr + suffix;
    }

    // =========================================================
    // 导出报价单 Excel
    // =========================================================
    @Override
    public void exportQuote(String quoteNo, List<String> columns, HttpServletResponse response) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");

            String dateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
            StringBuilder randomStr = new StringBuilder(5);
            for (int j = 0; j < 5; j++) {
                randomStr.append((char) ('A' + java.util.concurrent.ThreadLocalRandom.current().nextInt(26)));
            }
            String rawFileName = "Quotation_" + dateStr + "_" + randomStr;
            String fileName = java.net.URLEncoder.encode(rawFileName, java.nio.charset.StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

            List<QuoteDetail> detailList = quoteDetailMapper.selectList(
                    new QueryWrapper<QuoteDetail>()
                            .eq("quote_no", quoteNo)
                            .orderByAsc("item_index"));

            populateDescriptions(detailList);

            QuoteMain main = quoteMainMapper.selectOne(
                    new QueryWrapper<QuoteMain>().eq("quote_no", quoteNo));
            String currency = (main != null && main.getCurrency() != null) ? main.getCurrency() : "USD";
            String symbol = "RMB".equals(currency) ? "¥" : "$";

            List<String> specCodes = detailList.stream()
                    .map(QuoteDetail::getSpecCode)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            Map<String, Map<String, Object>> shapeInfoMap = Collections.emptyMap();
            if (!specCodes.isEmpty()) {
                shapeInfoMap = quoteDetailMapper.findShapeAndImageByCodes(specCodes)
                        .stream()
                        .filter(m -> m.get("specCode") != null)
                        .collect(Collectors.toMap(
                                m -> String.valueOf(m.get("specCode")),
                                m -> m,
                                (a, b) -> a
                        ));
            }

            // 4. 组装导出 DTO 列表
            List<QuoteExportDTO> exportList = new ArrayList<>(detailList.size());
            for (int i = 0; i < detailList.size(); i++) {
                QuoteExportDTO exportDTO = buildExportDTO(detailList.get(i), symbol, shapeInfoMap);
                exportList.add(exportDTO);
            }

            // ★ 安全校验：记录每个 ShapePrefix 首次出现的索引，保持原有的添加顺序
            Map<String, Integer> shapeFirstIndexMap = new HashMap<>();
            for (int i = 0; i < exportList.size(); i++) {
                QuoteExportDTO dto = exportList.get(i);
                // 使用和前端相同的正则提取确保稳定
                String shapePrefix = extractShapePrefix(dto.getSpecCode());
                shapeFirstIndexMap.putIfAbsent(shapePrefix, i);
            }

            // 执行排序
            exportList.sort((a, b) -> {
                String shapeA = extractShapePrefix(a.getSpecCode());
                String shapeB = extractShapePrefix(b.getSpecCode());

                // 先按类别首次出现的位置排序（新增类靠后）
                if (!shapeA.equals(shapeB)) {
                    return Integer.compare(shapeFirstIndexMap.get(shapeA), shapeFirstIndexMap.get(shapeB));
                }

                // 同类别下，按数字升序
                int numA = extractSizeNumber(a.getSpecCode());
                int numB = extractSizeNumber(b.getSpecCode());
                return Integer.compare(numA, numB);
            });

            // 重新分配 itemIndex 并计算 groupIndex 用于渲染斑马纹
            String currentShape = null;
            int groupIndex = -1;
            for (int i = 0; i < exportList.size(); i++) {
                QuoteExportDTO dto = exportList.get(i);
                dto.setItemIndex(i + 1);

                String shapePrefix = extractShapePrefix(dto.getSpecCode());
                if (!shapePrefix.equals(currentShape)) {
                    currentShape = shapePrefix;
                    groupIndex++;
                }
                dto.setGroupIndex(groupIndex);
            }

            // ★ 核心逻辑：解析前端传来的 columns 参数，计算需要排除（不导出）的列
            Set<String> excludeFields = new HashSet<>();
            List<String> validCols = (columns != null) ? columns : Collections.emptyList();
            if (!validCols.contains("weight")) excludeFields.add("weight");
            if (!validCols.contains("dimension")) excludeFields.add("dimension");
            if (!validCols.contains("price")) excludeFields.add("price");
            if (!validCols.contains("extraPrice")) excludeFields.add("extraPrice");
            if (!validCols.contains("cartonWeight")) excludeFields.add("cartonWeight");
            if (!validCols.contains("remarks")) excludeFields.add("remarks");

            // ========== 样式配置 ==========
            WriteCellStyle headStyle = new WriteCellStyle();
            headStyle.setFillPatternType(FillPatternType.SOLID_FOREGROUND);
            headStyle.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
            WriteFont headFont = new WriteFont();
            headFont.setFontName("Calibri");
            headFont.setFontHeightInPoints((short) 11);
            headFont.setBold(true);
            headStyle.setWriteFont(headFont);
            headStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
            headStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setCellBorder(headStyle);

            WriteCellStyle contentStyle = new WriteCellStyle();
            WriteFont contentFont = new WriteFont();
            contentFont.setFontName("Calibri");
            contentFont.setFontHeightInPoints((short) 10);
            contentStyle.setWriteFont(contentFont);
            contentStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
            contentStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            contentStyle.setWrapped(Boolean.TRUE);
            setCellBorder(contentStyle);

            HorizontalCellStyleStrategy styleStrategy =
                    new HorizontalCellStyleStrategy(headStyle, contentStyle);

            SimpleRowHeightStyleStrategy rowHeightStrategy =
                    new SimpleRowHeightStyleStrategy((short) 30, (short) 60);

            AbstractColumnWidthStyleStrategy columnWidthStrategy = buildColumnWidthStrategy();

            QuoteDynamicStyleCellWriteHandler styleHandler =
                    new QuoteDynamicStyleCellWriteHandler(currency, exportList);

            // ========== 写出 Excel ==========
            EasyExcel.write(response.getOutputStream(), QuoteExportDTO.class)
                    .excludeColumnFieldNames(excludeFields) // ★ 动态剔除未勾选字段
                    .registerWriteHandler(styleStrategy)
                    .registerWriteHandler(rowHeightStrategy)
                    .registerWriteHandler(columnWidthStrategy)
                    .registerWriteHandler(styleHandler)
                    .registerWriteHandler(new ShapeImageMergeStrategy(exportList))
                    .sheet("Quote Data")
                    .doWrite(exportList);

        } catch (Exception e) {
            log.error("[exportQuote] 导出失败, quoteNo={}", quoteNo, e);
            resetResponseOnError(response, e);
        }
    }

    /**
     * 【坚固优化】提取器型英文字母前缀（镜像前端正则算法）
     */
    private static String extractShapePrefix(String specCode) {
        if (specCode == null) return "";
        Matcher m = Pattern.compile("^([a-zA-Z]+)").matcher(specCode);
        if (m.find()) {
            return m.group(1);
        }
        return specCode;
    }

    /**
     * 【坚固优化】提取器型尺寸数字（镜像前端正则算法）
     */
    private static int extractSizeNumber(String specCode) {
        if (specCode == null) return 0;
        Matcher m = Pattern.compile("^([a-zA-Z]+)(\\d*)").matcher(specCode);
        if (m.find()) {
            String numStr = m.group(2);
            return (numStr != null && !numStr.isEmpty()) ? Integer.parseInt(numStr) : 0;
        }
        return 0;
    }

    /**
     * ★ 优化重构后的动态样式拦截器：彻底废弃原始 Index 识别，改为按 FieldName 稳定绑定
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

            // 1. 设置原生货币数字格式：通过字段名精确锁定单价和总额列，防止列减少后格式位移
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

    private QuoteExportDTO buildExportDTO(QuoteDetail detail, String symbol,
                                          Map<String, Map<String, Object>> shapeInfoMap) {
        QuoteExportDTO dto = new QuoteExportDTO();

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

        // ★ 新增：装填定制勾选列的基础数据
        dto.setWeight(detail.getWeight());
        dto.setDimension(detail.getDimension());
        dto.setPrice(detail.getOriginalPrice());
        dto.setExtraPrice(detail.getExtraPrice());
        dto.setCartonWeight(detail.getCartonWeight());
        dto.setRemarks(detail.getRemarks());

        Map<String, Object> shapeInfo = detail.getSpecCode() != null
                ? shapeInfoMap.get(detail.getSpecCode()) : null;

        if (shapeInfo != null && shapeInfo.get("shapeCode") != null) {
            dto.setShapeCode(String.valueOf(shapeInfo.get("shapeCode")));
            Object imgData = shapeInfo.get("imageData");
            if (imgData != null) {
                try {
                    if (imgData instanceof byte[]) {
                        byte[] bytes = (byte[]) imgData;
                        if (bytes.length > 0) dto.setPhoto(bytes);
                    } else if (imgData instanceof java.sql.Blob) {
                        java.sql.Blob blob = (java.sql.Blob) imgData;
                        dto.setPhoto(blob.getBytes(1, (int) blob.length()));
                    }
                } catch (Exception e) {
                    log.warn("[buildExportDTO] 解析图片失败，specCode={}", detail.getSpecCode(), e);
                }
            }
        } else {
            // ★ 安全校验：如果在库中没有匹配到该产品的器型图片信息，强制使用正则截取前缀做为后续的策略合并依据，保证和斑马纹保持同一维度！
            dto.setShapeCode(extractShapePrefix(detail.getSpecCode()));
        }

        if (detail.getUnitPrice() != null) {
            dto.setUnitPrice(detail.getUnitPrice());
        }

        if (detail.getCtns() != null && detail.getCtns() > 0) {
            BigDecimal ctnsDec = BigDecimal.valueOf(detail.getCtns());
            dto.setCtns(detail.getCtns());

            if (detail.getCbmCtn() != null) dto.setCbmTotal(detail.getCbmCtn().multiply(ctnsDec));
            if (detail.getGwCtn() != null) dto.setGwTotal(detail.getGwCtn().multiply(ctnsDec));
            if (detail.getNwCtn() != null) dto.setNwTotal(detail.getNwCtn().multiply(ctnsDec));
            if (dto.getTtlPcs() == null && detail.getPcs() != null) {
                dto.setTtlPcs(detail.getPcs() * detail.getCtns());
            }

            if (detail.getUnitPrice() != null && detail.getPcs() != null) {
                BigDecimal amt = detail.getUnitPrice()
                        .multiply(BigDecimal.valueOf(detail.getPcs()))
                        .multiply(ctnsDec)
                        .setScale(2, RoundingMode.HALF_UP);
                dto.setAmount(amt);
            }
        }

        return dto;
    }

    // =========================================================
    // 历史报价分页查询 (重构为查询主表 t_quote_main)
    // =========================================================
    @Override
    public Page<QuoteMain> getHistoryPage(Integer current, Integer size, String quoteNo, String remarks) {
        Page<QuoteMain> page = new Page<>(current, size);
        QueryWrapper<QuoteMain> wrapper = new QueryWrapper<>();

        if (quoteNo != null && !quoteNo.trim().isEmpty()) {
            wrapper.like("quote_no", quoteNo.trim().toUpperCase());
        }

        // 对主表的 remark 进行模糊匹配
        if (remarks != null && !remarks.trim().isEmpty()) {
            wrapper.like("remark", remarks.trim());
        }

        // 按更新时间或创建时间倒序排列
        wrapper.orderByDesc("update_time", "create_time");

        return quoteMainMapper.selectPage(page, wrapper);
    }

    @Override
    public List<QuoteDetail> getDetailsByQuoteNo(String quoteNo) {
        if (quoteNo == null || quoteNo.trim().isEmpty()) {
            return new ArrayList<>();
        }

        QueryWrapper<QuoteDetail> wrapper = new QueryWrapper<QuoteDetail>()
                .eq("quote_no", quoteNo.trim())
                .orderByAsc("item_index");

        List<QuoteDetail> list = quoteDetailMapper.selectList(wrapper);

        populateDescriptions(list);

        QuoteMain main = quoteMainMapper.selectOne(
                new QueryWrapper<QuoteMain>().eq("quote_no", quoteNo));
        String currency = main != null ? main.getCurrency() : "USD";

        for (QuoteDetail detail : list) {
            calculateAmount(detail);
            detail.setCurrency(currency);
        }

        return list;
    }

    private void calculateAmount(QuoteDetail detail) {
        if (detail.getUnitPrice() != null
                && detail.getPcs() != null
                && detail.getCtns() != null
                && detail.getCtns() > 0) {
            detail.setAmount(
                    detail.getUnitPrice()
                            .multiply(BigDecimal.valueOf(detail.getPcs()))
                            .multiply(BigDecimal.valueOf(detail.getCtns()))
            );
        }
    }

    private void populateDescriptions(List<QuoteDetail> list) {
        if (list == null || list.isEmpty()) return;

        List<String> specCodes = list.stream()
                .map(QuoteDetail::getSpecCode)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

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
            }
        }
    }
}