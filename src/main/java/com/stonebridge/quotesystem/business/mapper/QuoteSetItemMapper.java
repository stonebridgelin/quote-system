package com.stonebridge.quotesystem.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stonebridge.quotesystem.business.entity.QuoteSetItem;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QuoteSetItemMapper extends BaseMapper<QuoteSetItem> {

    @Insert({
            "<script>",
            "INSERT INTO t_quote_set_item (",
            "  id, quote_detail_id, sort_no, spec_code, design, weight,",
            "  original_price, qty_per_set, component_unit_price, create_time",
            ") VALUES",
            "<foreach collection='list' item='item' separator=','>",
            "(",
            "  #{item.id}, #{item.quoteDetailId}, #{item.sortNo}, #{item.specCode},",
            "  #{item.design}, #{item.weight}, #{item.originalPrice}, #{item.qtyPerSet},",
            "  #{item.componentUnitPrice}, #{item.createTime}",
            ")",
            "</foreach>",
            "</script>"
    })
    void insertBatch(@Param("list") List<QuoteSetItem> list);

    /** 即使数据库未启用级联外键，更新报价时也能先清理旧组件。 */
    @Delete({
            "DELETE item FROM t_quote_set_item item",
            "INNER JOIN t_quote_detail detail ON detail.id = item.quote_detail_id",
            "WHERE detail.quote_no = #{quoteNo}"
    })
    int deleteByQuoteNo(@Param("quoteNo") String quoteNo);
}
