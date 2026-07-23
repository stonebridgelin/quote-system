package com.stonebridge.quotesystem.business.entity.vo;

import lombok.Data;

@Data
public class OssPolicyVO {

    /**
     * OSS AccessKeyId，前端上传表单需要。
     * 注意：这里不是 AccessKeySecret。
     */
    private String accessid;

    /**
     * 上传策略
     */
    private String policy;

    /**
     * 上传签名
     */
    private String signature;

    /**
     * 上传目录
     */
    private String dir;

    /**
     * OSS host
     */
    private String host;

    /**
     * 过期时间，秒级时间戳
     */
    private String expire;
}