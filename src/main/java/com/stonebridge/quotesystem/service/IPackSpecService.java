package com.stonebridge.quotesystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.stonebridge.quotesystem.entity.dto.PackSpecInputDTO;
import com.stonebridge.quotesystem.entity.PackSpec;

public interface IPackSpecService extends IService<PackSpec> {
    void saveOrUpdateWithParse(PackSpecInputDTO dto);
}