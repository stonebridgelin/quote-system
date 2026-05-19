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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class QuoteServiceImpl implements IQuoteService {

    private QuoteMainMapper quoteMainMapper;
    private QuoteDetailMapper quoteDetailMapper;
    private IShapeSpecService shapeSpecService; // 用于反写吨价

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveQuote(QuoteSaveDTO dto) {
        String quoteNo = dto.getQuoteNo();
        boolean isUpdate = (quoteNo != null && !quoteNo.trim().isEmpty());

        if (isUpdate) {
            // 【执行更新逻辑】
            // 1. 更新主表
            UpdateWrapper<QuoteMain> mainUpdate = new UpdateWrapper<>();
            mainUpdate.eq("quote_no", quoteNo)
                    .set("currency", dto.getCurrency() != null ? dto.getCurrency() : "USD")
                    .set("exchange_rate", dto.getExchangeRate())
                    .set("remark", dto.getRemark());
            quoteMainMapper.update(null, mainUpdate);

            // 2. 清空旧明细 (全删全建策略)
            QueryWrapper<QuoteDetail> deleteWrapper = new QueryWrapper<>();
            deleteWrapper.eq("quote_no", quoteNo);
            quoteDetailMapper.delete(deleteWrapper);

        } else {
            // 【执行新增逻辑】
            String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            int randomNum = new Random().nextInt(900) + 100;
            quoteNo = "QT" + timeStr + randomNum;

            QuoteMain main = new QuoteMain();
            main.setQuoteNo(quoteNo);
            main.setCurrency(dto.getCurrency() != null ? dto.getCurrency() : "USD");
            main.setExchangeRate(dto.getExchangeRate());
            main.setRemark(dto.getRemark());
            quoteMainMapper.insert(main);
        }

        // 【统一步骤】：无论新增还是更新，都重新插入最新的明细
        List<QuoteDetail> details = dto.getDetailList();
        if (details != null && !details.isEmpty()) {
            int index = 1;
            for (QuoteDetail detail : details) {
                // 极其关键：清空前端可能传过来的历史 ID，强制作为新数据插入
                detail.setId(null);
                detail.setQuoteNo(quoteNo);
                detail.setItemIndex(index++);

                // 反写吨价逻辑保持不变
                if (detail.getOriginalPrice() != null && detail.getSpecCode() != null) {
                    UpdateWrapper<ShapeSpec> updateWrapper = new UpdateWrapper<>();
                    updateWrapper.eq("spec_code", detail.getSpecCode())
                            .set("ton_price", detail.getOriginalPrice());
                    shapeSpecService.update(updateWrapper);
                }

                quoteDetailMapper.insert(detail);
            }
        }

        return quoteNo;
    }

    @Override
    public void exportQuote(String quoteNo, HttpServletResponse response) {
        try {
            // 1. 设置响应头，告诉浏览器这是一个 Excel 下载流
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            // 防止中文文件名乱码
            String fileName = URLEncoder.encode("Quote_" + quoteNo, "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

            // 2. 从数据库查出该报价单的所有明细
            QueryWrapper<QuoteDetail> wrapper = new QueryWrapper<>();
            wrapper.eq("quote_no", quoteNo);
            List<QuoteDetail> detailList = quoteDetailMapper.selectList(wrapper);

            // 3. 查出主表信息，获取本次报价的币种，并决定使用哪个符号
            QuoteMain main = quoteMainMapper.selectOne(new QueryWrapper<QuoteMain>().eq("quote_no", quoteNo));
            String symbol = (main != null && "RMB".equals(main.getCurrency())) ? "¥" : "$";

            // 4. 将数据库实体转换为导出格式并补充计算
            List<QuoteExportDTO> exportList = new ArrayList<>();
            for (QuoteDetail detail : detailList) {
                QuoteExportDTO dto = new QuoteExportDTO();
                dto.setSpecCode(detail.getSpecCode());
                dto.setDescription(detail.getDescription());
                dto.setDesign(detail.getDesign());
                dto.setPcsPerSet(detail.getPcsPerSet());
                dto.setSetsPerCtn(detail.getSetsPerCtn());
                dto.setPcs(detail.getPcs());
                dto.setCbmCtn(detail.getCbmCtn());
                dto.setGwCtn(detail.getGwCtn());
                dto.setTtlPcs(detail.getTtlPcs());
                dto.setNwCtn(detail.getNwCtn());
                // 【高健壮性重构】：查器型代码和照片
                Map<String, Object> shapeInfo = quoteDetailMapper.findShapeAndImageBySpec(detail.getSpecCode());

                if (shapeInfo != null && shapeInfo.get("shapeCode") != null) {
                    // 1. 安全获取 shapeCode
                    dto.setShapeCode(String.valueOf(shapeInfo.get("shapeCode")));

                    // 2. 安全解析图片二进制流
                    Object imgData = shapeInfo.get("imageData");
                    if (imgData != null) {
                        try {
                            if (imgData instanceof byte[]) {
                                dto.setPhoto((byte[]) imgData);
                            } else if (imgData instanceof java.sql.Blob) {
                                // 兼容驱动返回 java.sql.Blob 的情况
                                java.sql.Blob blob = (java.sql.Blob) imgData;
                                dto.setPhoto(blob.getBytes(1, (int) blob.length()));
                            }
                        } catch (Exception e) {
                            // 吞掉异常，防止因为一张破损图片导致整个报价单下载失败
                            System.err.println("解析产品图片失败，SpecCode: " + detail.getSpecCode());
                        }
                    }
                } else {
                    // 【核心修复】：如果没查到主器型，直接用自己的 spec_code 作为 shapeCode，防止错误合并
                    dto.setShapeCode(detail.getSpecCode() != null ? detail.getSpecCode() : "EMPTY_SPEC");
                }

                // 给单价拼接货币符号
                if (detail.getUnitPrice() != null) {
                    dto.setUnitPrice(symbol + detail.getUnitPrice().toString());
                }

                // 计算箱数及汇总项
                if (detail.getCtns() != null && detail.getCtns() > 0) {
                    dto.setCtns(detail.getCtns()); // 这里对应的 Excel 列头已经是 "TTL CTNs"
                    BigDecimal ctnsDec = new BigDecimal(detail.getCtns());

                    if (detail.getCbmCtn() != null) {
                        dto.setCbmTotal(detail.getCbmCtn().multiply(ctnsDec));
                    }
                    if (detail.getGwCtn() != null) {
                        dto.setGwTotal(detail.getGwCtn().multiply(ctnsDec));
                    }
                    if (detail.getNwCtn() != null) {
                        dto.setNwTotal(detail.getNwCtn().multiply(ctnsDec));
                    }

                    // 防御性处理：如果数据库中保存的 ttlPcs 意外为空，可在导出时利用公式兜底计算一次
                    if (dto.getTtlPcs() == null && detail.getPcs() != null) {
                        dto.setTtlPcs(detail.getPcs() * detail.getCtns());
                    }

                    if (detail.getUnitPrice() != null && detail.getPcs() != null) {
                        BigDecimal pcsDec = new BigDecimal(detail.getPcs());
                        BigDecimal amt = detail.getUnitPrice().multiply(pcsDec).multiply(ctnsDec);
                        dto.setAmount(symbol + amt.setScale(2, java.math.RoundingMode.HALF_UP).toString());
                    }
                }

                exportList.add(dto);
            }

            // 【核心重排】：先按 shapeCode 升序 (聚拢同系列)，再按 specCode 降序 (排尺寸)
            exportList.sort((a, b) -> {
                int shapeComp = a.getShapeCode().compareTo(b.getShapeCode());
                if (shapeComp != 0) {
                    return shapeComp;
                }
                return b.getSpecCode().compareTo(a.getSpecCode());
            });

            // 【重写序号 & 图片去重】：
            // 重新从 1 开始排序号，同时只保留同组的第一张图片，防止多张图重叠！
            for (int i = 0; i < exportList.size(); i++) {
                exportList.get(i).setItemIndex(i + 1);
                // photo 不清空，ShapeImageMergeStrategy 自己按 shapeCode 分组，只绘制每组首行图片
            }


            // 5. 【注入灵魂】：注册自定义图片合并策略并导出
            EasyExcel.write(response.getOutputStream(), QuoteExportDTO.class)
                    .registerWriteHandler(new ShapeImageMergeStrategy(exportList)) // 挂载合并策略
                    .sheet("Quote Data")
                    .doWrite(exportList);

        } catch (IOException e) {
            throw new RuntimeException("导出Excel失败", e);
        }
    }

    @Override
    public Page<QuoteDetail> getHistoryPage(Integer current, Integer size, String quoteNo, String remarks) {
        Page<QuoteDetail> page = new Page<>(current, size);
        // 传递两个搜索条件给 Wrapper
        QueryWrapper<QuoteDetail> wrapper = getQuoteDetailQueryWrapper(quoteNo, remarks);

        quoteDetailMapper.selectPage(page, wrapper);

        // 1. 核心修复：删除了重复的一行 selectPage，避免执行两次相同的 SQL 查库
        quoteDetailMapper.selectPage(page, wrapper);

        // 2. 性能拦截：如果当前页根本没有查到数据，直接 return，省去后续无意义的循环判断
        List<QuoteDetail> records = page.getRecords();
        if (records == null || records.isEmpty()) {
            return page;
        }

        // 3. 遍历计算
        for (QuoteDetail detail : records) {
            detail.setPrice(detail.getOriginalPrice());

            // 使用提前定义好的条件判断，代码阅读起来更清晰
            boolean canCalculateAmount = detail.getUnitPrice() != null
                    && detail.getPcs() != null
                    && detail.getCtns() != null
                    && detail.getCtns() > 0;

            if (canCalculateAmount) {
                // 4. 内存优化：推荐使用 BigDecimal.valueOf() 而不是 new BigDecimal()
                BigDecimal pcsDec = BigDecimal.valueOf(detail.getPcs());
                BigDecimal ctnsDec = BigDecimal.valueOf(detail.getCtns());

                detail.setAmount(detail.getUnitPrice().multiply(pcsDec).multiply(ctnsDec));
            }
        }

        return page;
    }

    private static QueryWrapper<QuoteDetail> getQuoteDetailQueryWrapper(String quoteNo, String remarks) {
        QueryWrapper<QuoteDetail> wrapper = new QueryWrapper<>();

        // 1. 如果第一个框(单号/规格)有值，生成 OR 条件
        if (quoteNo != null && !quoteNo.trim().isEmpty()) {
            String searchKey = "%" + quoteNo.trim().toUpperCase() + "%";
            wrapper.and(w -> w
                    .like("spec_code", searchKey)
                    .or()
                    .like("quote_no", searchKey)
            );
        }

        // 2. 如果第二个框(备注)有值，生成 AND 独立的 LIKE 条件
        if (remarks != null && !remarks.trim().isEmpty()) {
            // 注意：这里直接 .like 即可，Mybatis Plus 默认会用 AND 连接前面的条件
            wrapper.like("remarks", "%" + remarks.trim() + "%");
        }

        wrapper.orderByDesc("create_time");
        return wrapper;
    }
}