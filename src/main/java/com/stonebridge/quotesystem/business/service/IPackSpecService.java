package com.stonebridge.quotesystem.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.stonebridge.quotesystem.business.entity.dto.PackSpecInputDTO;
import com.stonebridge.quotesystem.business.entity.PackSpec;

public interface IPackSpecService extends IService<PackSpec> {
    void saveOrUpdateWithParse(PackSpecInputDTO dto);
}