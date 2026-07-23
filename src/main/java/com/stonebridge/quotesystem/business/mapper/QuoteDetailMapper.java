package com.stonebridge.quotesystem.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stonebridge.quotesystem.business.entity.QuoteDetail;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface QuoteDetailMapper extends BaseMapper<QuoteDetail> {

    /**
     * 按单个 specCode 查询器型编码和图片数据（原有方法，保持不变）
     */
    @Select("SELECT sp.shape_code AS shapeCode, s.image_data AS imageData " +
            "FROM t_shape_spec sp " +
            "LEFT JOIN t_shape s ON sp.shape_code = s.shape_code " +
            "WHERE sp.spec_code = #{specCode} LIMIT 1")
    Map<String, Object> findShapeAndImageBySpec(@Param("specCode") String specCode);

    /**
     * 批量插入报价明细
     * 字段严格对照 QuoteDetail 实体（排除 @TableField(exist=false) 的虚拟字段）
     * id 与 create_time 均由 Service 层预先生成，保证自定义批量 SQL 字段完整。
     * ★ 修复：已将 description 字段从插入语句和 values 中移除
     */
    @Insert({
            "<script>",
            "INSERT INTO t_quote_detail (",
            "  id, order_id, quote_no, item_index, line_type, set_group_id, set_name, set_unit_price,",
            "  spec_code, design,",
            "  pcs_per_set, sets_per_ctn, pcs, ctns, ttl_pcs,",
            "  cbm_ctn, gw_ctn, nw_ctn,",
            "  unit_price, original_price, extra_price,",
            "  weight, dimension, carton_weight, remarks, create_time",
            ") VALUES",
            "<foreach collection='list' item='d' separator=','>",
            "(",
            "  #{d.id}, #{d.orderId}, #{d.quoteNo}, #{d.itemIndex}, #{d.lineType},",
            "  #{d.setGroupId}, #{d.setName}, #{d.setUnitPrice}, #{d.specCode}, #{d.design},",
            "  #{d.pcsPerSet}, #{d.setsPerCtn}, #{d.pcs}, #{d.ctns}, #{d.ttlPcs},",
            "  #{d.cbmCtn}, #{d.gwCtn}, #{d.nwCtn},",
            "  #{d.unitPrice}, #{d.originalPrice}, #{d.extraPrice},",
            "  #{d.weight}, #{d.dimension}, #{d.cartonWeight}, #{d.remarks}, #{d.createTime}",
            ")",
            "</foreach>",
            "</script>"
    })
    void insertBatch(@Param("list") List<QuoteDetail> list);

    /**
     * 批量查询多个 specCode 的器型编码和图片数据（替代 N+1 查询）
     * 返回 Map 包含：specCode、shapeCode、imageData
     */
    @Select({
            "<script>",
            "SELECT sp.spec_code AS specCode, sp.shape_code AS shapeCode, s.image_data AS imageData",
            "FROM t_shape_spec sp",
            "LEFT JOIN t_shape s ON sp.shape_code = s.shape_code",
            "WHERE sp.spec_code IN",
            "<foreach collection='codes' item='code' open='(' separator=',' close=')'>",
            "  #{code}",
            "</foreach>",
            "</script>"
    })
    List<Map<String, Object>> findShapeAndImageByCodes(@Param("codes") List<String> specCodes);
}
