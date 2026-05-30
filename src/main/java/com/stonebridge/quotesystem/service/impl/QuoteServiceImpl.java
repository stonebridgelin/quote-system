package com.stonebridge.quotesystem.service.impl;

import com.alibaba.excel.EasyExcel;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class QuoteServiceImpl implements IQuoteService {

    private QuoteMainMapper quoteMainMapper;
    private QuoteDetailMapper quoteDetailMapper;
    private IShapeSpecService shapeSpecService;

    @Autowired
    public void setQuoteMainMapper(QuoteMainMapper quoteMainMapper) {
        this.quoteMainMapper = quoteMainMapper;
    }

    @Autowired
    public void setQuoteDetailMapper(QuoteDetailMapper quoteDetailMapper) {
        this.quoteDetailMapper = quoteDetailMapper;
    }

    @Autowired
    public void setShapeSpecService(IShapeSpecService shapeSpecService) {
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
            // 【优化】使用 Set 记录当前批次已经更新过吨价的 specCode，避免重复执行 SQL
            Set<String> updatedSpecs = new HashSet<>();
            for (int i = 0; i < details.size(); i++) {
                QuoteDetail detail = details.get(i);
                detail.setId(null);
                detail.setQuoteNo(quoteNo);
                detail.setItemIndex(i + 1);
                // 反写吨价到基础资料表（增加去重判断）
                if (detail.getOriginalPrice() != null && detail.getSpecCode() != null) {
                    if (!updatedSpecs.contains(detail.getSpecCode())) {
                        shapeSpecService.update(new UpdateWrapper<ShapeSpec>()
                                .eq("spec_code", detail.getSpecCode())
                                .set("ton_price", detail.getOriginalPrice()));
                        updatedSpecs.add(detail.getSpecCode()); // 标记该器型已更新
                    }
                }
                quoteDetailMapper.insert(detail);
                // 提示：如果你实现了 MyBatis-Plus 的 IService，此处可以把集合存起来
                // 循环结束后统一调用 saveBatch(details) 批量插入，性能会成倍提升
            }
        }

        return quoteNo;
    }

    public static String getQuoteID() {
        // 1. 获取 UUID
        String uuid = UUID.randomUUID().toString();
        // 2. 获取哈希码并取绝对值
        int hashCode = Math.abs(uuid.hashCode());
        // 3. 将哈希值限制在 26^3 (17576) 的范围内
        int alphaRange = hashCode % 17576;
        // 4. 将数字转换为 3 位全小写字母 (如果需要大写，可将 'a' 改为 'A')
        char[] letters = new char[3];
        letters[0] = (char) ('a' + (alphaRange / 676) % 26); // 676 即 26 * 26
        letters[1] = (char) ('a' + (alphaRange / 26) % 26);
        letters[2] = (char) ('a' + alphaRange % 26);
        String threeLetterHash = new String(letters);
        // 5. 获取时间字符串 (MMddHHmmss)
        String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmmss"));
        // 6. 拼接返回
        return timeStr + threeLetterHash.toUpperCase();
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
            String fileName = URLEncoder.encode("Quote_" + quoteNo, "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

            // 2. 【FIX-顺序】按 item_index 升序查询，严格还原用户保存时的行顺序
            //    去掉了原来按 shapeCode/specCode 强制重排的逻辑，改由前端/用户决定顺序
            QueryWrapper<QuoteDetail> wrapper = new QueryWrapper<QuoteDetail>()
                    .eq("quote_no", quoteNo)
                    .orderByAsc("item_index");
            List<QuoteDetail> detailList = quoteDetailMapper.selectList(wrapper);

            // 3. 查主表获取币种符号
            QuoteMain main = quoteMainMapper.selectOne(
                    new QueryWrapper<QuoteMain>().eq("quote_no", quoteNo));
            String symbol = (main != null && "RMB".equals(main.getCurrency())) ? "¥" : "$";

            // 4. 组装导出 DTO 列表
            List<QuoteExportDTO> exportList = new ArrayList<>();
            for (QuoteDetail detail : detailList) {
                QuoteExportDTO dto = buildExportDTO(detail, symbol);
                exportList.add(dto);
            }

            // 5. 按 item_index 重写序号（数据库已排好序，直接 1..N 写入即可）
            for (int i = 0; i < exportList.size(); i++) {
                exportList.get(i).setItemIndex(i + 1);
            }

            // 6. 注册图片合并策略并写出 Excel
            EasyExcel.write(response.getOutputStream(), QuoteExportDTO.class)
                    .registerWriteHandler(new ShapeImageMergeStrategy(exportList))
                    .sheet("Quote Data")
                    .doWrite(exportList);

        } catch (IOException e) {
            throw new RuntimeException("导出Excel失败", e);
        }
    }

    /**
     * 将单条明细实体组装成导出 DTO，含图片查询和汇总计算。
     * 单独抽出方法，让 exportQuote 主流程更清晰。
     */
    private QuoteExportDTO buildExportDTO(QuoteDetail detail, String symbol) {
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

        // 查器型图片（健壮处理，单张图片异常不影响整单导出）
        Map<String, Object> shapeInfo = quoteDetailMapper.findShapeAndImageBySpec(detail.getSpecCode());
        if (shapeInfo != null && shapeInfo.get("shapeCode") != null) {
            dto.setShapeCode(String.valueOf(shapeInfo.get("shapeCode")));
            Object imgData = shapeInfo.get("imageData");
            if (imgData != null) {
                try {
                    if (imgData instanceof byte[]) {
                        dto.setPhoto((byte[]) imgData);
                    } else if (imgData instanceof java.sql.Blob) {
                        java.sql.Blob blob = (java.sql.Blob) imgData;
                        dto.setPhoto(blob.getBytes(1, (int) blob.length()));
                    }
                } catch (Exception e) {
                    System.err.println("[exportQuote] 解析图片失败，specCode=" + detail.getSpecCode());
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
                        .setScale(2, java.math.RoundingMode.HALF_UP);
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
        // 【FIX】删除了原来重复执行的第二次 selectPage，避免双倍数据库查询

        List<QuoteDetail> records = page.getRecords();
        if (records == null || records.isEmpty()) {
            return page;
        }

        // 补算金额（历史列表展示用）
        for (QuoteDetail detail : records) {
            detail.setPrice(detail.getOriginalPrice());
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

        // 【FIX-顺序】按 item_index 排序，保证载入时行顺序和原单一致
        QueryWrapper<QuoteDetail> wrapper = new QueryWrapper<QuoteDetail>()
                .eq("quote_no", quoteNo.trim())
                .orderByAsc("item_index");


        List<QuoteDetail> list = quoteDetailMapper.selectList(wrapper);
        QuoteMain main = quoteMainMapper.selectOne(
                new QueryWrapper<QuoteMain>().eq("quote_no", quoteNo));
        String currency = main != null ? main.getCurrency() : "USD";

        // 补算金额
        for (QuoteDetail detail : list) {
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
            detail.setCurrency(currency); // QuoteDetail 加一个 transient 字段
        }

        return list;
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