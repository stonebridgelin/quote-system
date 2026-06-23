package com.stonebridge.quotesystem.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stonebridge.quotesystem.common.Result;
import com.stonebridge.quotesystem.entity.SampleTracking;
import com.stonebridge.quotesystem.entity.dto.SampleSaveDTO;
import com.stonebridge.quotesystem.service.ISampleTrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sample")
@CrossOrigin
public class SampleTrackingController {

    @Autowired
    private ISampleTrackingService sampleTrackingService;

    /**
     * 1. 复杂查询与智能排序列表
     */
    /**
     * 1. 复杂查询与智能排序列表 (带分页)
     */
    @GetMapping("/list")
    public Result<Page<SampleTracking>> getList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "15") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<String> statusList) {

        Page<SampleTracking> page = sampleTrackingService.getListPage(current, size, keyword, statusList);
        return Result.success(page);
    }

    /**
     * 2. 新增或更新样品单
     */
    @PostMapping("/save")
    public Result<String> saveOrUpdate(@RequestBody SampleSaveDTO dto) {
        try {
            sampleTrackingService.saveOrUpdate(dto);
            return Result.success("保存成功");
        } catch (RuntimeException e) {
            // 捕获 Service 层抛出的 "数据不存在" 等业务异常
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 3. 获取单条明细及图片
     */
    @GetMapping("/detail/{id}")
    public Result<SampleSaveDTO> getDetail(@PathVariable Long id) {
        try {
            SampleSaveDTO dto = sampleTrackingService.getDetail(id);
            return Result.success(dto);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }
}