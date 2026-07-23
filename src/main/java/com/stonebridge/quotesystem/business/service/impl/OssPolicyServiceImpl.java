package com.stonebridge.quotesystem.business.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.model.MatchMode;
import com.aliyun.oss.model.PolicyConditions;
import com.stonebridge.quotesystem.common.OssPathUtil;
import com.stonebridge.quotesystem.config.AliyunOssProperties;
import com.stonebridge.quotesystem.business.entity.vo.OssPolicyVO;
import com.stonebridge.quotesystem.business.service.OssPolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class OssPolicyServiceImpl implements OssPolicyService {

    private final AliyunOssProperties ossProperties;

    @Override
    public OssPolicyVO getReviewFormPolicy(String orderId) {
        String dir = OssPathUtil.buildReviewFormDir(ossProperties.getDirPrefix(), orderId);
        return buildPolicy(dir);
    }

    @Override
    public OssPolicyVO getLogImagePolicy(String orderId, String moduleType) {
        String dir = OssPathUtil.buildLogImageDir(ossProperties.getDirPrefix(), orderId, moduleType);
        return buildPolicy(dir);
    }

    private OssPolicyVO buildPolicy(String dir) {
        OSS ossClient = new OSSClientBuilder().build(
                ossProperties.getEndpoint(),
                ossProperties.getAccessKeyId(),
                ossProperties.getAccessKeySecret()
        );

        try {
            long expireSeconds = ossProperties.getExpireSeconds() == null
                    ? 300L
                    : ossProperties.getExpireSeconds();

            long expireEndTime = System.currentTimeMillis() + expireSeconds * 1000;
            Date expiration = new Date(expireEndTime);

            long maxSizeMb = ossProperties.getMaxSizeMb() == null
                    ? 10L
                    : ossProperties.getMaxSizeMb();

            long maxSizeBytes = maxSizeMb * 1024 * 1024;

            PolicyConditions policyConditions = new PolicyConditions();
            policyConditions.addConditionItem(
                    PolicyConditions.COND_CONTENT_LENGTH_RANGE,
                    0,
                    maxSizeBytes
            );
            policyConditions.addConditionItem(
                    MatchMode.StartWith,
                    PolicyConditions.COND_KEY,
                    dir
            );

            String postPolicy = ossClient.generatePostPolicy(expiration, policyConditions);
            String encodedPolicy = BinaryUtil.toBase64String(
                    postPolicy.getBytes(StandardCharsets.UTF_8)
            );
            String postSignature = ossClient.calculatePostSignature(postPolicy);

            OssPolicyVO vo = new OssPolicyVO();
            vo.setAccessid(ossProperties.getAccessKeyId());
            vo.setPolicy(encodedPolicy);
            vo.setSignature(postSignature);
            vo.setDir(dir);
            vo.setHost(buildHost());
            vo.setExpire(String.valueOf(expireEndTime / 1000));

            return vo;
        } catch (Exception e) {
            log.error("获取 OSS 上传签名失败", e);
            throw new RuntimeException("获取 OSS 上传签名失败");
        } finally {
            ossClient.shutdown();
        }
    }

    private String buildHost() {
        if (ossProperties.getUrlPrefix() != null && !ossProperties.getUrlPrefix().trim().isEmpty()) {
            return ossProperties.getUrlPrefix().replaceAll("/+$", "");
        }
        return "https://" + ossProperties.getBucket() + "." + ossProperties.getEndpoint();
    }
}