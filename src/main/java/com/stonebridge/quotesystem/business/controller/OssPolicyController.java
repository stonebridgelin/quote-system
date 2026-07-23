package com.stonebridge.quotesystem.business.controller;

import com.stonebridge.quotesystem.common.Result;
import com.stonebridge.quotesystem.business.entity.vo.OssPolicyVO;
import com.stonebridge.quotesystem.business.service.OssPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/system/aliyun")
@RequiredArgsConstructor
public class OssPolicyController {

    private final OssPolicyService ossPolicyService;

    /**
     * 获取合同评审表截图上传签名
     *
     * 说明：
     * 这是 OSS 上传签名能力，不直接归属于 quote 权限。
     * 只要用户具备 oss:policy 权限，即可获取上传签名。
     */
    @PreAuthorize("hasAuthority('oss:policy')")
    @GetMapping("/review-form-policy")
    public Result<OssPolicyVO> getReviewFormPolicy(
            @RequestParam(required = false) String orderId
    ) {
        return Result.success(ossPolicyService.getReviewFormPolicy(orderId));
    }

    /**
     * 获取订单追踪日志图片上传签名
     *
     * 说明：
     * 虽然当前用于订单追踪日志图片，但本质仍是 OSS 上传签名能力。
     */
    @PreAuthorize("hasAuthority('oss:policy')")
    @GetMapping("/log-image-policy")
    public Result<OssPolicyVO> getLogImagePolicy(
            @RequestParam(required = false) String orderId,
            @RequestParam String moduleType
    ) {
        return Result.success(ossPolicyService.getLogImagePolicy(orderId, moduleType));
    }
}