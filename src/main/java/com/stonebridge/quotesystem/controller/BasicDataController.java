package com.stonebridge.quotesystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stonebridge.quotesystem.common.Result;
import com.stonebridge.quotesystem.entity.PackSpec;
import com.stonebridge.quotesystem.entity.dto.PackSpecInputDTO;
import com.stonebridge.quotesystem.entity.Shape;
import com.stonebridge.quotesystem.entity.ShapeSpec;
import com.stonebridge.quotesystem.entity.SysConfig;
import com.stonebridge.quotesystem.entity.dto.ShapeSpecImportDTO;
import com.stonebridge.quotesystem.mapper.ShapeMapper;
import com.stonebridge.quotesystem.mapper.SysConfigMapper;
import com.stonebridge.quotesystem.service.IPackSpecService;
import com.stonebridge.quotesystem.service.IShapeSpecService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.alibaba.excel.EasyExcel;
import com.stonebridge.quotesystem.listener.ShapeSpecExcelListener;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/basic")
@CrossOrigin
public class BasicDataController {

    @Autowired private ShapeMapper shapeMapper;
    @Autowired private IShapeSpecService shapeSpecService;
    @Autowired private IPackSpecService packSpecService;
    @Autowired private SysConfigMapper sysConfigMapper;

    // --- 器型表维护 ---
    @GetMapping("/shape/page")
    public Map<String, Object> getShapePage(@RequestParam(defaultValue = "1") Integer current,
                                            @RequestParam(defaultValue = "10") Integer size) {
        Page<Shape> page = new Page<>(current, size);
        shapeMapper.selectPage(page, null);
        return success(page);
    }

    // 在 BasicDataController 中补充以下方法：

    @DeleteMapping("/shape-spec/{id}")
    public Result<String> deleteShapeSpec(@PathVariable Long id) {
        // 调用 MyBatis-Plus 的 removeById 删除
        boolean success = shapeSpecService.removeById(id);
        if (success) {
            return Result.success("删除成功");
        } else {
            return Result.fail("删除失败，数据可能不存在");
        }
    }

    @PostMapping("/shape/save")
    public Map<String, Object> saveShape(@RequestBody Shape shape) {
        if(shape.getId() == null) shapeMapper.insert(shape);
        else shapeMapper.updateById(shape);
        return success(null);
    }

    // --- 器型规格表维护 (调用带解析的 Service) ---
    @PostMapping("/shape-spec/save")
    public Map<String, Object> saveShapeSpec(@RequestBody ShapeSpec shapeSpec) {
        shapeSpecService.saveOrUpdateWithParse(shapeSpec);
        return success(null);
    }

    // --- 包装规格表维护 (调用带解析的 Service) ---
    @PostMapping("/pack-spec/save")
    public Map<String, Object> savePackSpec(@RequestBody PackSpecInputDTO dto) {
        packSpecService.saveOrUpdateWithParse(dto);
        return success(null);
    }

    // --- 获取与修改汇率 ---
    @GetMapping("/config/rate")
    public Map<String, Object> getRate() {
        QueryWrapper<SysConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("config_key", "USD_EXCHANGE_RATE");
        SysConfig config = sysConfigMapper.selectOne(wrapper);
        return success(config != null ? config.getConfigValue() : "6.8");
    }

    @PostMapping("/config/rate")
    public Map<String, Object> updateRate(@RequestParam String value) {
        QueryWrapper<SysConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("config_key", "USD_EXCHANGE_RATE");
        SysConfig config = sysConfigMapper.selectOne(wrapper);
        config.setConfigValue(value);
        sysConfigMapper.updateById(config);
        return success(null);
    }

    // 快捷返回封装
    private Map<String, Object> success(Object data) {
        Map<String, Object> res = new HashMap<>();
        res.put("code", 200);
        res.put("data", data);
        return res;
    }

    @PostMapping("/shape-spec/import")
    public Result<String> importShapeSpec(@RequestParam("file") MultipartFile file) throws Exception {
        // 专注业务逻辑，如果有异常直接抛出，GlobalExceptionHandler 会自动接管！
        EasyExcel.read(file.getInputStream(), ShapeSpecImportDTO.class,
                new ShapeSpecExcelListener(shapeSpecService)).sheet().doRead();

        return Result.success("器型规格导入成功", null);
    }

    // 在 BasicDataController 类中补充以下代码


    @GetMapping("/shape-spec/page")
    public Result<Page<ShapeSpec>> getShapeSpecPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,     // 接收分页大小 (10)
            @RequestParam(required = false) String specCode,
            @RequestParam(required = false) String sizeVal) {    // 接收物理尺寸 (如 "8.5")

        Page<ShapeSpec> page = new Page<>(current, size);
        QueryWrapper<ShapeSpec> wrapper = new QueryWrapper<>();

        // 如果前端传了 specCode，进行模糊查询
        if (specCode != null && !specCode.isEmpty()) {
            wrapper.like("spec_code", specCode);
        }
        // 【关键点】：如果前端传了尺寸，进行精确匹配
        if (sizeVal != null && !sizeVal.isEmpty()) {
            wrapper.eq("size", sizeVal);
        }

        // 自定义排序：先按系列聚类，再按尺寸从大到小
        wrapper.last("ORDER BY shape_code ASC, size + 0 DESC");

        shapeSpecService.page(page, wrapper);
        return Result.success(page);
    }

    // --- 2. 包装规格表分页查询 ---
    @GetMapping("/pack-spec/page")
    public Result<Page<PackSpec>> getPackSpecPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String packModel) { // 仅保留 packModel

        Page<PackSpec> page = new Page<>(current, size);
        QueryWrapper<PackSpec> wrapper = new QueryWrapper<>();

        packModel = packModel.trim();
        if (!packModel.trim().isEmpty()) {
            if (packModel.startsWith("F")){
                packModel= packModel.replaceFirst("F","");
            }
            wrapper.like("spec_code", packModel.trim());
        }

        wrapper.orderByDesc("create_time");
        packSpecService.page(page, wrapper);
        return Result.success(page);
    }
    // 在 BasicDataController.java 中添加以下代码

    @DeleteMapping("/shape/{id}")
    public Result<String> deleteShape(@PathVariable Long id) {
        // 检查是否有该数据
        Shape shape = shapeMapper.selectById(id);
        if (shape == null) {
            return Result.fail("该器型不存在，可能已被删除");
        }

        // 执行逻辑删除或物理删除
        shapeMapper.deleteById(id);
        return Result.success("删除成功");
    }

    @DeleteMapping("/pack-spec/{id}")
    public Result<String> deletePackSpec(@PathVariable Long id) {
        boolean success = packSpecService.removeById(id);
        if (success) {
            return Result.success("删除成功");
        } else {
            return Result.fail("删除失败，该包装方案可能已被删除");
        }
    }
}