package com.stonebridge.quotesystem.listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.stonebridge.quotesystem.entity.dto.ShapeSpecImportDTO;
import com.stonebridge.quotesystem.entity.ShapeSpec;
import com.stonebridge.quotesystem.service.IShapeSpecService;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ShapeSpecExcelListener implements ReadListener<ShapeSpecImportDTO> {

    private final IShapeSpecService shapeSpecService;
    private static final int BATCH_COUNT = 100; // 每隔100条存储数据库，防止内存占用过多
    private List<ShapeSpecImportDTO> cachedDataList = new ArrayList<>(BATCH_COUNT);

    // 构造函数传入 Service
    public ShapeSpecExcelListener(IShapeSpecService shapeSpecService) {
        this.shapeSpecService = shapeSpecService;
    }

    @Override
    public void invoke(ShapeSpecImportDTO data, AnalysisContext context) {
        cachedDataList.add(data);
        if (cachedDataList.size() >= BATCH_COUNT) {
            saveData();
            cachedDataList = new ArrayList<>(BATCH_COUNT);
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // 保存最后一批数据
        saveData();
        log.info("所有器型规格解析完成！");
    }

    private void saveData() {
        log.info("{} 条器型规格数据被解析，开始入库...", cachedDataList.size());
        for (ShapeSpecImportDTO dto : cachedDataList) {
            ShapeSpec entity = new ShapeSpec();
            entity.setShapeCode(dto.getShapeCode());
            entity.setSpecCode(dto.getSpecCode());
            entity.setDescription(dto.getDescription());
            entity.setTonPrice(dto.getTonPrice());
            try {
                entity.setWeight(new BigDecimal(dto.getWeight()));
            } catch (Exception e) {
                entity.setWeight(BigDecimal.ZERO); // 容错处理
            }
            // 调用之前写好的方法：这里会自动提取 "8.5" 并入库
            shapeSpecService.saveOrUpdateWithParse(entity);
        }
    }
}