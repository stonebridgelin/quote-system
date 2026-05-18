package com.stonebridge.quotesystem.service;

import com.stonebridge.quotesystem.entity.vo.UploadResultVO;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

public interface IShapeService {
    UploadResultVO uploadShapeImages(MultipartFile[] files);
}
