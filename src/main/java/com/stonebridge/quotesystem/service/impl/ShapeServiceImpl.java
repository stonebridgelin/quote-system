package com.stonebridge.quotesystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.stonebridge.quotesystem.entity.Shape;
import com.stonebridge.quotesystem.entity.vo.UploadResultVO;
import com.stonebridge.quotesystem.mapper.ShapeMapper;
import com.stonebridge.quotesystem.service.IShapeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ShapeServiceImpl implements IShapeService {

    private ShapeMapper shapeMapper;
    @Autowired
    public void setShapeMapper(ShapeMapper shapeMapper) {
        this.shapeMapper = shapeMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public UploadResultVO uploadShapeImages(MultipartFile[] files) {
        UploadResultVO result = new UploadResultVO();

        for (MultipartFile file : files) {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) continue;

            // 1. 校验后缀名
            String lowerName = originalFilename.toLowerCase();
            if (!lowerName.endsWith(".jpg") && !lowerName.endsWith(".png")) {
                result.getErrorList().add(originalFilename + " (格式错误，仅支持jpg/png)");
                continue;
            }

            // 2. 截取文件名作为 shape_code (例如: "mlpp.jpg" -> "mlpp")
            String shapeCode = originalFilename.substring(0, originalFilename.lastIndexOf("."));
            // 强转为大写以匹配数据库 (MLPP)
            String upperShapeCode = shapeCode.toUpperCase();

            try {
                // 3. 构造更新条件
                UpdateWrapper<Shape> updateWrapper = new UpdateWrapper<>();
                updateWrapper.eq("shape_code", upperShapeCode);

                // 【注意】：为了不查全表，我们直接尝试 Update
                // 如果 update 影响的行数 > 0，说明数据库有这个器型；如果是 0，说明没这个器型
                Shape updateEntity = new Shape();
                updateEntity.setImageData(file.getBytes());

                int rows = shapeMapper.update(updateEntity, updateWrapper);

                if (rows > 0) {
                    result.getSuccessList().add(originalFilename);
                } else {
                    result.getErrorList().add(originalFilename + " (数据库未找到该器型代码: " + upperShapeCode + ")");
                }
            } catch (Exception e) {
                result.getErrorList().add(originalFilename + " (文件读取异常)");
            }
        }
        return result;
    }
}
