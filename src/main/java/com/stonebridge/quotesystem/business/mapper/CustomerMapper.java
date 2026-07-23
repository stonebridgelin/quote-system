package com.stonebridge.quotesystem.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stonebridge.quotesystem.business.entity.Customer;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {

    /**
     * 唯一索引配合 INSERT IGNORE，避免并发新增相同客户时使整个订单事务失败。
     */
    @Insert("""
            INSERT IGNORE INTO t_customer (id, customer_name, create_time)
            VALUES (#{customer.id}, #{customer.customerName}, #{customer.createTime})
            """)
    int insertIgnore(@Param("customer") Customer customer);
}
