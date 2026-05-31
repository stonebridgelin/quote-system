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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 报价单 Service 实现
 *
 * 优化记录：
 * 1. getQuoteID()        - 修复 Integer.MIN_VALUE 溢出 bug，改用 UUID 末 6 位保证唯一性
 * 2. saveQuote()         - 明细改为批量插入，消除 N 次单条 INSERT 性能问题
 * 3. exportQuote()       - 引入 slf4j 日志，消除异常静默吞掉和 printStackTrace
 * 4. buildExportDTO()    - 改为接收预查询的 shapeInfoMap，消除 N+1 查询问题
 * 5. calculateAmount()   - 抽取金额补算公共方法，消除 getHistoryPage/getDetailsByQuoteNo 重复代码
 * 6. buildColumnWidthStrategy() - 修正图片列判断逻辑（原用 EMPTY 类型语义错误）
 * 7. 依赖注入            - 改为构造器注入，字段 final，符合 Spring 规范
 * 8. 格式化修复           - 采用 EasyExcel 官方推荐的 WriteCellData 注入方式，完美解决动态货币符号丢失问题
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

    // =========================================================
    // 保存报价单（新增 / 全删全建更新）
    // =========================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveQuote(QuoteSaveDTO dto) {
        String quoteNo = dto.getQuoteNo();
        boolean isUpdate = (quoteNo != null && !quoteNo.trim().isEmpty());

        if (isUpdate) {
            UpdateWrapper<QuoteMain> mainUpdate = new UpdateWrapper<>();
            mainUpdate.eq("quote_no", quoteNo)
                    .set("currency", dto.getCurrency() != null ? dto.getCurrency() : "USD")
                    .set("exchange_rate", dto.getExchangeRate())
                    .set("remark", dto.getRemark());
            quoteMainMapper.update(null, mainUpdate);

            quoteDetailMapper.delete(new QueryWrapper<QuoteDetail>().eq("quote_no", quoteNo));
        } else {
            quoteNo = getQuoteID();
            QuoteMain main = new QuoteMain();
            main.setQuoteNo(quoteNo);
            main.setCurrency(dto.getCurrency() != null ? dto.getCurrency() : "USD");
            main.setExchangeRate(dto.getExchangeRate());
            main.setRemark(dto.getRemark());
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
    public void exportQuote(String quoteNo, HttpServletResponse response) {
        try {
            // 1. 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("Quote_" + quoteNo, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

            // 2. 按 item_index 升序查询明细
            List<QuoteDetail> detailList = quoteDetailMapper.selectList(
                    new QueryWrapper<QuoteDetail>()
                            .eq("quote_no", quoteNo)
                            .orderByAsc("item_index"));

            // 3. 查主表获取币种及符号
            QuoteMain main = quoteMainMapper.selectOne(
                    new QueryWrapper<QuoteMain>().eq("quote_no", quoteNo));
            String currency = (main != null && main.getCurrency() != null) ? main.getCurrency() : "USD";
            String symbol = "RMB".equals(currency) ? "¥" : "$";

            // 批量查询所有器型图片，消除 N+1 查询
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
                exportDTO.setItemIndex(i + 1);
                exportList.add(exportDTO);
            }

            // ========== 样式配置 ==========

            // ① 表头样式
            WriteCellStyle headStyle = new WriteCellStyle();
            headStyle.setFillPatternType(FillPatternType.SOLID_FOREGROUND);
            headStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            WriteFont headFont = new WriteFont();
            headFont.setFontName("Calibri");
            headFont.setFontHeightInPoints((short) 11);
            headFont.setBold(true);
            headStyle.setWriteFont(headFont);
            headStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
            headStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setCellBorder(headStyle);

            // ② 内容样式
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

            // ③ 行高策略
            SimpleRowHeightStyleStrategy rowHeightStrategy =
                    new SimpleRowHeightStyleStrategy((short) 30, (short) 60);

            // ④ 自定义列宽策略
            AbstractColumnWidthStyleStrategy columnWidthStrategy = buildColumnWidthStrategy();

            // ⑤ 实例化我们重构后的格式拦截器 (16: U.PRICE, 17: AMOUNT)
            CurrencyFormatCellWriteHandler currencyFormatHandler =
                    new CurrencyFormatCellWriteHandler(Arrays.asList(16, 17), currency);

            // ========== 写出 Excel ==========
            EasyExcel.write(response.getOutputStream(), QuoteExportDTO.class)
                    .registerWriteHandler(styleStrategy)
                    .registerWriteHandler(rowHeightStrategy)
                    .registerWriteHandler(columnWidthStrategy)
                    .registerWriteHandler(currencyFormatHandler) // 注册全新的拦截器
                    .registerWriteHandler(new ShapeImageMergeStrategy(exportList))
                    .sheet("Quote Data")
                    .doWrite(exportList);

        } catch (Exception e) {
            log.error("[exportQuote] 导出失败, quoteNo={}", quoteNo, e);
            resetResponseOnError(response, e);
        }
    }

    /**
     * 【全新重构】自定义单元格格式拦截器
     *
     * 核心改变：不再强行修改 POI 的 CellStyle（会被流式导出覆盖），
     * 而是利用 EasyExcel 的 afterCellDataConverted 钩子，
     * 直接修改 WriteCellData 的底层属性，让 EasyExcel 的样式字典帮我们安全地渲染格式。
     */
    private static class CurrencyFormatCellWriteHandler implements CellWriteHandler {
        private final List<Integer> targetColumnIndexes;
        private final String currency;

        public CurrencyFormatCellWriteHandler(List<Integer> targetColumnIndexes, String currency) {
            this.targetColumnIndexes = targetColumnIndexes;
            this.currency = currency;
        }

        @Override
        public void afterCellDataConverted(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder,
                                           WriteCellData<?> cellData, Cell cell, Head head, Integer relativeRowIndex, Boolean isHead) {
            // 仅拦截非表头，且在我们指定的目标列中
            if (Boolean.FALSE.equals(isHead) && cellData != null && targetColumnIndexes.contains(cell.getColumnIndex())) {

                // 拿到 EasyExcel 当前为这个单元格准备好的样式对象
                WriteCellStyle writeCellStyle = cellData.getOrCreateStyle();

                // 获取或初始化数据格式属性
                DataFormatData dataFormatData = writeCellStyle.getDataFormatData();
                if (dataFormatData == null) {
                    dataFormatData = new DataFormatData();
                    writeCellStyle.setDataFormatData(dataFormatData);
                }

                // 使用最标准、直白的带转义货币格式，交给引擎处理
                String formatStr = "RMB".equals(currency) ? "\"¥\"#,##0.00" : "\"$\"#,##0.00";
                dataFormatData.setFormat(formatStr);
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
                if (cellDataList == null || cellDataList.isEmpty()) {
                    return;
                }

                if (head != null && (head.getHeadNameList() == null
                        || head.getHeadNameList().stream().allMatch(s -> s == null || s.isEmpty()))) {
                    return;
                }

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

    /**
     * 将单条明细实体组装成导出 DTO
     */
    private QuoteExportDTO buildExportDTO(QuoteDetail detail, String symbol,
                                          Map<String, Map<String, Object>> shapeInfoMap) {
        QuoteExportDTO dto = new QuoteExportDTO();

        // 基础字段映射
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

        // 从预查 Map 中取器型图片
        Map<String, Object> shapeInfo = detail.getSpecCode() != null
                ? shapeInfoMap.get(detail.getSpecCode()) : null;

        if (shapeInfo != null && shapeInfo.get("shapeCode") != null) {
            dto.setShapeCode(String.valueOf(shapeInfo.get("shapeCode")));
            Object imgData = shapeInfo.get("imageData");
            if (imgData != null) {
                try {
                    if (imgData instanceof byte[]) {
                        byte[] bytes = (byte[]) imgData;
                        if (bytes.length > 0) {
                            dto.setPhoto(bytes);
                        }
                    } else if (imgData instanceof java.sql.Blob) {
                        java.sql.Blob blob = (java.sql.Blob) imgData;
                        dto.setPhoto(blob.getBytes(1, (int) blob.length()));
                    }
                } catch (Exception e) {
                    log.warn("[buildExportDTO] 解析图片失败，specCode={}", detail.getSpecCode(), e);
                }
            }
        } else {
            dto.setShapeCode(detail.getSpecCode() != null ? detail.getSpecCode() : "EMPTY_SPEC");
        }

        // 单价（直接放入 BigDecimal，拦截器负责格式化）
        if (detail.getUnitPrice() != null) {
            dto.setUnitPrice(detail.getUnitPrice());
        }

        // 箱数相关汇总
        if (detail.getCtns() != null && detail.getCtns() > 0) {
            BigDecimal ctnsDec = BigDecimal.valueOf(detail.getCtns());
            dto.setCtns(detail.getCtns());

            if (detail.getCbmCtn() != null) {
                dto.setCbmTotal(detail.getCbmCtn().multiply(ctnsDec));
            }
            if (detail.getGwCtn() != null) {
                dto.setGwTotal(detail.getGwCtn().multiply(ctnsDec));
            }
            if (detail.getNwCtn() != null) {
                dto.setNwTotal(detail.getNwCtn().multiply(ctnsDec));
            }

            if (dto.getTtlPcs() == null && detail.getPcs() != null) {
                dto.setTtlPcs(detail.getPcs() * detail.getCtns());
            }

            // 金额（直接放入 BigDecimal，拦截器负责格式化）
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
    // 历史报价分页查询
    // =========================================================
    @Override
    public Page<QuoteDetail> getHistoryPage(Integer current, Integer size, String quoteNo, String remarks) {
        Page<QuoteDetail> page = new Page<>(current, size);
        quoteDetailMapper.selectPage(page, getQuoteDetailQueryWrapper(quoteNo, remarks));

        List<QuoteDetail> records = page.getRecords();
        if (records == null || records.isEmpty()) {
            return page;
        }

        for (QuoteDetail detail : records) {
            detail.setPrice(detail.getOriginalPrice());
            calculateAmount(detail);
        }

        return page;
    }

    // =========================================================
    // 按单号查整单明细
    // =========================================================
    @Override
    public List<QuoteDetail> getDetailsByQuoteNo(String quoteNo) {
        if (quoteNo == null || quoteNo.trim().isEmpty()) {
            return new ArrayList<>();
        }

        QueryWrapper<QuoteDetail> wrapper = new QueryWrapper<QuoteDetail>()
                .eq("quote_no", quoteNo.trim())
                .orderByAsc("item_index");

        List<QuoteDetail> list = quoteDetailMapper.selectList(wrapper);
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

    private static QueryWrapper<QuoteDetail> getQuoteDetailQueryWrapper(String quoteNo, String remarks) {
        QueryWrapper<QuoteDetail> wrapper = new QueryWrapper<>();

        if (quoteNo != null && !quoteNo.trim().isEmpty()) {
            String key = "%" + quoteNo.trim().toUpperCase() + "%";
            wrapper.and(w -> w.like("spec_code", key).or().like("quote_no", key));
        }

        if (remarks != null && !remarks.trim().isEmpty()) {
            wrapper.like("remarks", "%" + remarks.trim() + "%");
        }

        wrapper.orderByDesc("create_time");
        return wrapper;
    }
}