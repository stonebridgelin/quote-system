package com.stonebridge.quotesystem.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
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
 */
@Service
public class QuoteServiceImpl implements IQuoteService {

    private static final Logger log = LoggerFactory.getLogger(QuoteServiceImpl.class);

    // ★ 优化7：构造器注入，字段 final，符合 Spring 官方推荐规范，便于单元测试
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
            // 更新主表基本信息
            UpdateWrapper<QuoteMain> mainUpdate = new UpdateWrapper<>();
            mainUpdate.eq("quote_no", quoteNo)
                    .set("currency", dto.getCurrency() != null ? dto.getCurrency() : "USD")
                    .set("exchange_rate", dto.getExchangeRate())
                    .set("remark", dto.getRemark());
            quoteMainMapper.update(null, mainUpdate);

            // 全删旧明细（全删全建策略，简单可靠）
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

        // 统一：按前端传入顺序插入明细，item_index 即为前端显示的行号
        List<QuoteDetail> details = dto.getDetailList();
        if (details != null && !details.isEmpty()) {
            // 使用 Set 记录当前批次已经更新过吨价的 specCode，避免重复执行 SQL
            Set<String> updatedSpecs = new HashSet<>();
            // ★ 优化2：先收集待插入列表，循环结束后统一批量插入，消除 N 次单条 INSERT
            List<QuoteDetail> toInsert = new ArrayList<>(details.size());

            for (int i = 0; i < details.size(); i++) {
                QuoteDetail detail = details.get(i);
                detail.setId(null);
                detail.setQuoteNo(quoteNo);
                detail.setItemIndex(i + 1);

                // 反写吨价到基础资料表（去重，避免重复 UPDATE 同一 specCode）
                if (detail.getOriginalPrice() != null && detail.getSpecCode() != null
                        && !updatedSpecs.contains(detail.getSpecCode())) {
                    shapeSpecService.update(new UpdateWrapper<ShapeSpec>()
                            .eq("spec_code", detail.getSpecCode())
                            .set("ton_price", detail.getOriginalPrice()));
                    updatedSpecs.add(detail.getSpecCode());
                }

                toInsert.add(detail);
            }

            // 批量插入（需在 QuoteDetailMapper 中添加 insertBatch XML 方法，
            // 或将 Service 改为继承 ServiceImpl 后调用 saveBatch(toInsert, 500)）
            quoteDetailMapper.insertBatch(toInsert);
        }

        return quoteNo;
    }

    /**
     * 生成报价单号：时间前缀（MMddHHmmss）+ UUID 末 6 位大写
     *
     * ★ 优化1：
     *   原实现用 UUID.hashCode() % 17576 映射 3 位字母，哈希空间仅 17576 种，
     *   同一秒内高并发时极易碰撞；且 Math.abs(Integer.MIN_VALUE) 仍为负数，存在越界 bug。
     *   改为直接截取去连字符 UUID 末 6 位（16^6 ≈ 1600 万种），碰撞概率极低。
     */
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
        // ★ 优化3：统一使用 slf4j 日志，去除 printStackTrace 和异常静默吞掉
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

            // 3. 查主表获取币种符号
            QuoteMain main = quoteMainMapper.selectOne(
                    new QueryWrapper<QuoteMain>().eq("quote_no", quoteNo));
            String symbol = (main != null && "RMB".equals(main.getCurrency())) ? "¥" : "$";

            // ★ 优化4：批量查询所有器型图片，消除 buildExportDTO 内的 N+1 查询
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
                                (a, b) -> a  // 同 specCode 重复时保留第一条
                        ));
            }

            // 4. 组装导出 DTO 列表（传入预查好的 shapeInfoMap，无需再单条查询）
            List<QuoteExportDTO> exportList = new ArrayList<>(detailList.size());
            for (int i = 0; i < detailList.size(); i++) {
                QuoteExportDTO exportDTO = buildExportDTO(detailList.get(i), symbol, shapeInfoMap);
                exportDTO.setItemIndex(i + 1); // 直接在此处设置序号，省去后续二次遍历
                exportList.add(exportDTO);
            }

            // ========== 样式配置 ==========

            // ① 表头样式 —— 必须显式设置 fillPatternType，否则背景色不生效
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

            // ④ 自定义列宽策略（跳过图片列，避免与 ShapeImageMergeStrategy 冲突）
            AbstractColumnWidthStyleStrategy columnWidthStrategy = buildColumnWidthStrategy();

            // ========== 写出 Excel ==========
            EasyExcel.write(response.getOutputStream(), QuoteExportDTO.class)
                    .registerWriteHandler(styleStrategy)
                    .registerWriteHandler(rowHeightStrategy)
                    .registerWriteHandler(columnWidthStrategy)
                    .registerWriteHandler(new ShapeImageMergeStrategy(exportList))
                    .sheet("Quote Data")
                    .doWrite(exportList);

        } catch (Exception e) {
            // ★ 优化3：记录完整异常堆栈，便于线上排查
            log.error("[exportQuote] 导出失败, quoteNo={}", quoteNo, e);
            resetResponseOnError(response, e);
        }
    }

    /**
     * 自定义列宽策略：
     * - 跳过图片列（head 名为空时视为图片占位列），防止与 ShapeImageMergeStrategy 冲突
     * - 其他列按内容自适应，宽度限制在 [10, 60]，防止列过宽
     *
     * ★ 优化6：原代码用 CellDataTypeEnum.EMPTY 判断图片列，语义错误（EMPTY 表示空单元格）。
     *   修正为：若 head 的 headNameList 全为空则视为图片列并跳过，语义更准确。
     *   根本解法是在 QuoteExportDTO 的 photo 字段加 @ExcelIgnore，由 ShapeImageMergeStrategy
     *   全权写入图片，彻底消除冲突。
     */
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

                // ★ 优化6：通过 head 名称列表是否全为空来识别图片占位列，语义正确
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
                            // 中文字符按 2 个宽度计算
                            dataLength = cellData.getStringValue().chars()
                                    .map(c -> c > 127 ? 2 : 1)
                                    .sum();
                        } else if (cellData.getNumberValue() != null) {
                            dataLength = cellData.getNumberValue().toPlainString().length();
                        }
                    }
                }

                if (dataLength == 0) return;

                // 取历史最大值，限制在 [10, 60] 范围内
                int maxWidth = Math.min(Math.max(
                        columnWidthMap.getOrDefault(columnIndex, 10),
                        dataLength + 2  // +2 作为 padding
                ), 60);

                columnWidthMap.put(columnIndex, maxWidth);
                writeSheetHolder.getSheet().setColumnWidth(columnIndex, maxWidth * 256);
            }
        };
    }

    /**
     * 导出异常时重置响应，避免客户端收到损坏的 Excel 文件
     */
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
     * 将单条明细实体组装成导出 DTO，含图片填充和汇总计算。
     *
     * ★ 优化4：接收外部预查好的 shapeInfoMap，不再内部单条查 DB，消除 N+1 问题。
     *
     * @param detail      明细实体
     * @param symbol      币种符号（¥ / $）
     * @param shapeInfoMap key=specCode，value=器型+图片信息（由 findShapeAndImageByCodes 批量查出）
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

        // 从预查 Map 中取器型图片（单张异常不影响整单导出）
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
            // 查不到主器型时用 specCode 自身占位，防止图片错误合并
            dto.setShapeCode(detail.getSpecCode() != null ? detail.getSpecCode() : "EMPTY_SPEC");
        }

        // 单价（带币种符号）
        if (detail.getUnitPrice() != null) {
            dto.setUnitPrice(symbol + detail.getUnitPrice().toPlainString());
        }

        // 箱数相关汇总（只有 ctns > 0 才计算，防止除零和无意义数据）
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

            // ttlPcs 兜底：数据库若为空则在导出时补算
            if (dto.getTtlPcs() == null && detail.getPcs() != null) {
                dto.setTtlPcs(detail.getPcs() * detail.getCtns());
            }

            // 金额（带币种符号，保留两位小数）
            if (detail.getUnitPrice() != null && detail.getPcs() != null) {
                BigDecimal amt = detail.getUnitPrice()
                        .multiply(BigDecimal.valueOf(detail.getPcs()))
                        .multiply(ctnsDec)
                        .setScale(2, RoundingMode.HALF_UP);
                dto.setAmount(symbol + amt.toPlainString());
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

        // ★ 优化5：金额补算逻辑复用公共方法 calculateAmount，消除重复代码
        for (QuoteDetail detail : records) {
            detail.setPrice(detail.getOriginalPrice());
            calculateAmount(detail);
        }

        return page;
    }

    // =========================================================
    // 按单号查整单明细（用于历史弹窗"载入整单"功能）
    // =========================================================
    @Override
    public List<QuoteDetail> getDetailsByQuoteNo(String quoteNo) {
        if (quoteNo == null || quoteNo.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // 按 item_index 排序，保证载入时行顺序和原单一致
        QueryWrapper<QuoteDetail> wrapper = new QueryWrapper<QuoteDetail>()
                .eq("quote_no", quoteNo.trim())
                .orderByAsc("item_index");

        List<QuoteDetail> list = quoteDetailMapper.selectList(wrapper);
        QuoteMain main = quoteMainMapper.selectOne(
                new QueryWrapper<QuoteMain>().eq("quote_no", quoteNo));
        String currency = main != null ? main.getCurrency() : "USD";

        // ★ 优化5：金额补算逻辑复用公共方法，消除重复代码
        for (QuoteDetail detail : list) {
            calculateAmount(detail);
            detail.setCurrency(currency); // QuoteDetail 需加一个 transient 字段
        }

        return list;
    }

    /**
     * 补算明细金额（unitPrice × pcs × ctns）
     *
     * ★ 优化5：抽取公共方法，供 getHistoryPage 和 getDetailsByQuoteNo 复用，遵循 DRY 原则。
     */
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

    // =========================================================
    // 构造历史查询条件
    // =========================================================
    private static QueryWrapper<QuoteDetail> getQuoteDetailQueryWrapper(String quoteNo, String remarks) {
        QueryWrapper<QuoteDetail> wrapper = new QueryWrapper<>();

        // 单号 / 规格代码 模糊匹配（OR 关系）
        if (quoteNo != null && !quoteNo.trim().isEmpty()) {
            String key = "%" + quoteNo.trim().toUpperCase() + "%";
            wrapper.and(w -> w.like("spec_code", key).or().like("quote_no", key));
        }

        // 备注模糊匹配（AND 关系，独立条件）
        if (remarks != null && !remarks.trim().isEmpty()) {
            wrapper.like("remarks", "%" + remarks.trim() + "%");
        }

        wrapper.orderByDesc("create_time");
        return wrapper;
    }
}