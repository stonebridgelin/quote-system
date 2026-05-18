package com.stonebridge.quotesystem.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.stonebridge.quotesystem.entity.ShapeSpec;

public interface IShapeSpecService extends IService<ShapeSpec> {
    void saveOrUpdateWithParse(ShapeSpec shapeSpec);
}