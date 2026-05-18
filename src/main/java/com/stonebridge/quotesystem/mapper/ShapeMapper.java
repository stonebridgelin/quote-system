package com.stonebridge.quotesystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stonebridge.quotesystem.entity.Shape;
import org.apache.ibatis.annotations.Mapper;

@Mapper // 告诉 Spring Boot 这是一个 Mapper 接口
public interface ShapeMapper extends BaseMapper<Shape> {
    // BaseMapper 里已经包含了 insert, delete, update, selectById 等常用方法
}