package com.stonebridge.quotesystem.controller;

import com.stonebridge.quotesystem.common.Result;
import com.stonebridge.quotesystem.entity.vo.UploadResultVO;
import com.stonebridge.quotesystem.service.IShapeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/shape")
@CrossOrigin // 解决 Vue 前端跨域
public class ShapeController {

    private IShapeService shapeService;

    @Autowired
    public void setShapeService(IShapeService shapeService) {
        this.shapeService = shapeService;
    }

    @PostMapping("/upload-images")
    public Result<UploadResultVO> uploadShapeImages(@RequestParam("files") MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return Result.fail("请选择要上传的图片");
        }
        UploadResultVO result = shapeService.uploadShapeImages(files);
        return Result.success(result);
    }
}
