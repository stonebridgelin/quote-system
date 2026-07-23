package com.stonebridge.quotesystem.business.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stonebridge.quotesystem.business.entity.SampleTracking;
import com.stonebridge.quotesystem.business.entity.dto.SampleSaveDTO;

import java.util.List;

public interface ISampleTrackingService {
    /**
     * 复杂查询、智能排序并分页
     */
    Page<SampleTracking> getListPage(Integer current, Integer size, String keyword, List<String> statusList);

    /**
     * 新增或更新样品单
     */
    void saveOrUpdate(SampleSaveDTO dto);

    /**
     * 获取单条明细及图片
     */
    SampleSaveDTO getDetail(Long id);

    void removeById(Long id);
}
