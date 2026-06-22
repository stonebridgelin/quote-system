package com.stonebridge.quotesystem.service;

import com.stonebridge.quotesystem.entity.SampleTracking;
import com.stonebridge.quotesystem.entity.dto.SampleSaveDTO;

import java.util.List;

public interface ISampleTrackingService {
    /**
     * 复杂查询与智能排序列表
     */
    List<SampleTracking> getList(String keyword, List<String> statusList);

    /**
     * 新增或更新样品单
     */
    void saveOrUpdate(SampleSaveDTO dto);

    /**
     * 获取单条明细及图片
     */
    SampleSaveDTO getDetail(Long id);
}
