package com.stonebridge.quotesystem.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.stonebridge.quotesystem.business.entity.ShapeSpec;

public interface IShapeSpecService extends IService<ShapeSpec> {
    void saveOrUpdateWithParse(ShapeSpec shapeSpec);
}