package com.stonebridge.quotesystem.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "aliyun.oss")
public class AliyunOssProperties {

    /**
     * OSS endpoint，例如：oss-cn-shanghai.aliyuncs.com
     */
    private String endpoint;

    /**
     * Bucket 名称
     */
    private String bucket;

    /**
     * AccessKeyId
     */
    private String accessKeyId;

    /**
     * AccessKeySecret
     */
    private String accessKeySecret;

    /**
     * 图片访问前缀，例如：
     * https://xxx.oss-cn-shanghai.aliyuncs.com
     */
    private String urlPrefix;

    /**
     * 业务目录前缀，例如：order-tracking
     */
    private String dirPrefix = "order-tracking";

    /**
     * 签名有效期，单位秒
     */
    private Long expireSeconds = 300L;

    /**
     * 单张图片最大大小，单位 MB
     */
    private Long maxSizeMb = 10L;
}