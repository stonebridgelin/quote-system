package com.stonebridge.quotesystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.stonebridge.quotesystem.entity.PackSpec;
import com.stonebridge.quotesystem.entity.Shape;
import com.stonebridge.quotesystem.entity.ShapeSpec;
import com.stonebridge.quotesystem.mapper.PackSpecMapper;
import com.stonebridge.quotesystem.mapper.ShapeMapper;
import com.stonebridge.quotesystem.mapper.ShapeSpecMapper;
import com.stonebridge.quotesystem.service.IQuoteInteractionService;
import com.stonebridge.quotesystem.entity.vo.PackOptionVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuoteInteractionServiceImpl implements IQuoteInteractionService {

    private ShapeMapper shapeMapper;
    private ShapeSpecMapper shapeSpecMapper;
    private PackSpecMapper packSpecMapper;

    @Autowired
    public void setShapeMapper(ShapeMapper shapeMapper) {
        this.shapeMapper = shapeMapper;
    }

    @Autowired
    public void setShapeSpecMapper(ShapeSpecMapper shapeSpecMapper) {
        this.shapeSpecMapper = shapeSpecMapper;
    }

    @Autowired
    public void setPackSpecMapper(PackSpecMapper packSpecMapper) {
        this.packSpecMapper = packSpecMapper;
    }

    @Override
    public List<Shape> searchShape(String keyword) {
        QueryWrapper<Shape> wrapper = new QueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            // 支持对器型代号或中文名称进行模糊匹配
            wrapper.like("shape_code", keyword).or().like("shape_name", keyword);
        }
        wrapper.last("LIMIT 20"); // 限制返回条数，提升前端渲染性能
        return shapeMapper.selectList(wrapper);
    }

    @Override
    public List<ShapeSpec> getShapeSpecsByShapeCode(String shapeCode) {
        QueryWrapper<ShapeSpec> wrapper = new QueryWrapper<>();
        wrapper.eq("shape_code", shapeCode);
        List<ShapeSpec> list = shapeSpecMapper.selectList(wrapper);

        // 核心难点 1：因为 size 在数据库是 varchar (比如 "8.5")，直接用 SQL 的 order by 可能会按照字符串排 (8.5 会排在 10 后面)。
        // 解决方案：在 Java 内存中将其转换为 Double 进行升序排序。
        return list.stream()
                .sorted((a, b) -> {
                    double sizeA = a.getSize() == null || a.getSize().isEmpty() ? 0.0 : Double.parseDouble(a.getSize());
                    double sizeB = b.getSize() == null || b.getSize().isEmpty() ? 0.0 : Double.parseDouble(b.getSize());
                    return Double.compare(sizeA, sizeB);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<PackOptionVO> getPackOptions(String specCode, BigDecimal weight) {
        // 1. 查询该规格代码下的所有基础包装方案
        QueryWrapper<PackSpec> wrapper = new QueryWrapper<>();
        wrapper.eq("spec_code", specCode);
        List<PackSpec> packSpecs = packSpecMapper.selectList(wrapper);

        // 2. 利用 Java 8 Stream API 进行动态计算和重新映射
        return packSpecs.stream().map(pack -> {
                    PackOptionVO vo = new PackOptionVO();
                    // 将基础属性拷贝到 VO 中
                    BeanUtils.copyProperties(pack, vo);

                    // 安全处理 null 值
                    int pcsPerBox = pack.getPcsPerBox() == null ? 0 : pack.getPcsPerBox();
                    int boxesPerCtn = pack.getBoxesPerCarton() == null ? 0 : pack.getBoxesPerCarton();
                    int pcs = pcsPerBox * boxesPerCtn;
                    BigDecimal safeWeight = weight == null ? BigDecimal.ZERO : weight;

                    // --- 核心算法：计算毛重 G.W. ctn ---
                    // 公式：(PCS * weight / 1000) + 1.1
                    BigDecimal gwCtn = new BigDecimal(pcs).multiply(safeWeight)
                            .divide(new BigDecimal("1000"), 4, RoundingMode.HALF_UP)
                            .add(new BigDecimal("1.1"))
                            .setScale(2, RoundingMode.HALF_UP);
                    vo.setGwCtn(gwCtn);

                    // --- 顺手计算体积 CBM ctn ---
                    BigDecimal l = pack.getOuterLength() != null ? pack.getOuterLength() : BigDecimal.ZERO;
                    BigDecimal w = pack.getOuterWidth() != null ? pack.getOuterWidth() : BigDecimal.ZERO;
                    BigDecimal h = pack.getOuterHeight() != null ? pack.getOuterHeight() : BigDecimal.ZERO;
                    BigDecimal cbmCtn = l.multiply(w).multiply(h)
                            .divide(new BigDecimal("1000000"), 3, RoundingMode.HALF_UP);
                    vo.setCbmCtn(cbmCtn);

                    return vo;

                })
                // 核心难点 2：根据刚刚计算出的动态毛重 (gwCtn) 进行降序 (reversed) 排列
                .sorted(Comparator.comparing(PackOptionVO::getGwCtn).reversed())
                .collect(Collectors.toList());
    }
}
