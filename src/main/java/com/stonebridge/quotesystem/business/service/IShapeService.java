package com.stonebridge.quotesystem.business.service;

import com.stonebridge.quotesystem.business.entity.vo.UploadResultVO;
import org.springframework.web.multipart.MultipartFile;

public interface IShapeService {
    UploadResultVO uploadShapeImages(MultipartFile[] files);
}
