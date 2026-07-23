package com.stonebridge.quotesystem.business.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stonebridge.quotesystem.business.entity.ShapeSpec;
import com.stonebridge.quotesystem.business.mapper.ShapeSpecMapper;
import com.stonebridge.quotesystem.business.service.IShapeSpecService;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ShapeSpecServiceImpl extends ServiceImpl<ShapeSpecMapper, ShapeSpec> implements IShapeSpecService {

    private static final Pattern SIZE_PATTERN = Pattern.compile("(\\d+)$"); // 匹配末尾数字

    @Override
    public void saveOrUpdateWithParse(ShapeSpec shapeSpec) {
        // 核心解析逻辑：从器型规格代码(如 FLXRKP85) 中提取末尾数字解析为尺寸 (8.5)
        if (shapeSpec.getSpecCode() != null) {
            Matcher matcher = SIZE_PATTERN.matcher(shapeSpec.getSpecCode());
            if (matcher.find()) {
                String numberStr = matcher.group(1);
                // 默认规则：提取到的数字除以10即为英寸大小（85 -> 8.5）
                double size = Double.parseDouble(numberStr) / 10.0;
                shapeSpec.setSize(size + "");
            }
        }
        // 调用 MyBatis-Plus 提供的保存或更新方法
        this.saveOrUpdate(shapeSpec);
    }
}