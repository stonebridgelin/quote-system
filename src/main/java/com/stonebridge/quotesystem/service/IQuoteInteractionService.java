package com.stonebridge.quotesystem.service;

import com.stonebridge.quotesystem.entity.Shape;
import com.stonebridge.quotesystem.entity.ShapeSpec;
import com.stonebridge.quotesystem.entity.vo.PackOptionVO;

import java.math.BigDecimal;
import java.util.List;

public interface IQuoteInteractionService {

    // Level 2弹窗：器型模糊查询
    List<Shape> searchShape(String keyword);

    // Level 1弹窗：根据器型代号查询所有规格，按尺寸升序
    List<ShapeSpec> getShapeSpecsByShapeCode(String shapeCode);

    // Level 3弹窗：根据规格代码和重量，动态计算毛重并降序排列包装方案
    List<PackOptionVO> getPackOptions(String specCode, BigDecimal weight);
}