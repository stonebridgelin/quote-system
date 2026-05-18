package com.stonebridge.quotesystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stonebridge.quotesystem.entity.QuoteDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface QuoteDetailMapper extends BaseMapper<QuoteDetail> {
    // 【修改点】：改为 sp.shape_code，并强制加上 AS shapeCode 和 AS imageData
    @Select("SELECT sp.shape_code AS shapeCode, s.image_data AS imageData " +
            "FROM t_shape_spec sp " +
            "LEFT JOIN t_shape s ON sp.shape_code = s.shape_code " +
            "WHERE sp.spec_code = #{specCode} LIMIT 1")
    Map<String, Object> findShapeAndImageBySpec(@Param("specCode") String specCode);
}