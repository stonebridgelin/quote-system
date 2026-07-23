package com.stonebridge.quotesystem.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stonebridge.quotesystem.business.entity.SignedOrderSetItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SignedOrderSetItemMapper extends BaseMapper<SignedOrderSetItem> {

    @Insert({
            "<script>",
            "INSERT INTO t_signed_order_set_item (",
            "  id, order_detail_id, sort_no, product_code, weight, qty_per_set, create_time",
            ") VALUES",
            "<foreach collection='list' item='item' separator=','>",
            "(",
            "  #{item.id}, #{item.orderDetailId}, #{item.sortNo}, #{item.productCode},",
            "  #{item.weight}, #{item.qtyPerSet}, #{item.createTime}",
            ")",
            "</foreach>",
            "</script>"
    })
    int insertBatch(@Param("list") List<SignedOrderSetItem> list);
}
