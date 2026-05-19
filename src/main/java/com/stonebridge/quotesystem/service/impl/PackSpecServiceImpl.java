package com.stonebridge.quotesystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stonebridge.quotesystem.entity.dto.PackSpecInputDTO;
import com.stonebridge.quotesystem.entity.PackSpec;
import com.stonebridge.quotesystem.mapper.PackSpecMapper;
import com.stonebridge.quotesystem.service.IPackSpecService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PackSpecServiceImpl extends ServiceImpl<PackSpecMapper, PackSpec> implements IPackSpecService {

    @Override
    public void saveOrUpdateWithParse(PackSpecInputDTO dto) {
        PackSpec packSpec = new PackSpec();
        packSpec.setId(dto.getId());
        packSpec.setPackModel(dto.getPackModel());

        // 1. 拆分包装型号 (如 "XP80-6-8外箱" -> 提取 XP80, 6, 8)
        if (dto.getPackModel() != null) {
            String cleanStr = dto.getPackModel().replace("外箱", ""); // 剔除多余文字
            String[] parts = cleanStr.split("-");
            if (parts.length >= 3) {
                packSpec.setSpecCode(parts[0]);
                packSpec.setPcsPerBox(Integer.parseInt(parts[1]));
                packSpec.setBoxesPerCarton(Integer.parseInt(parts[2]));
            }
        }

        // 2. 拆分外箱尺寸 (如 "54.6*28.2*15.1")
        if (dto.getOuterSizeStr() != null && dto.getOuterSizeStr().contains("*")) {
            String[] outerDims = dto.getOuterSizeStr().split("\\*");
            if (outerDims.length == 3) {
                packSpec.setOuterLength(new BigDecimal(outerDims[0].trim()));
                packSpec.setOuterWidth(new BigDecimal(outerDims[1].trim()));
                packSpec.setOuterHeight(new BigDecimal(outerDims[2].trim()));
            }
        }

        // 3. 拆分黄盒尺寸 (如 "13.3*13.2*13.3")
        if (dto.getInnerSizeStr() != null && dto.getInnerSizeStr().contains("*")) {
            String[] innerDims = dto.getInnerSizeStr().split("\\*");
            if (innerDims.length == 3) {
                packSpec.setInnerLength(new BigDecimal(innerDims[0].trim()));
                packSpec.setInnerWidth(new BigDecimal(innerDims[1].trim()));
                packSpec.setInnerHeight(new BigDecimal(innerDims[2].trim()));
            }
        }

        this.saveOrUpdate(packSpec);
    }
}