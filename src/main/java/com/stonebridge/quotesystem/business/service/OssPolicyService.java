package com.stonebridge.quotesystem.business.service;

import com.stonebridge.quotesystem.business.entity.vo.OssPolicyVO;

public interface OssPolicyService {

    /**
     * 获取合同评审表截图上传签名
     */
    OssPolicyVO getReviewFormPolicy(String orderId);

    /**
     * 获取日志图片上传签名
     */
    OssPolicyVO getLogImagePolicy(String orderId, String moduleType);
}