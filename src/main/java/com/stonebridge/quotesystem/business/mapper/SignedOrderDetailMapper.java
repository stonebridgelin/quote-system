package com.stonebridge.quotesystem.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stonebridge.quotesystem.business.entity.SignedOrderDetail;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SignedOrderDetailMapper extends BaseMapper<SignedOrderDetail> {

    @Insert({
            "<script>",
            "INSERT INTO t_signed_order_detail (",
            "  id, order_id, sort_no, line_type, product_code, weight,",
            "  total_pcs, total_sets, unit_price, amount, create_time",
            ") VALUES",
            "<foreach collection='list' item='item' separator=','>",
            "(",
            "  #{item.id}, #{item.orderId}, #{item.sortNo}, #{item.lineType},",
            "  #{item.productCode}, #{item.weight}, #{item.totalPcs}, #{item.totalSets},",
            "  #{item.unitPrice}, #{item.amount}, #{item.createTime}",
            ")",
            "</foreach>",
            "</script>"
    })
    int insertBatch(@Param("list") List<SignedOrderDetail> list);
}
