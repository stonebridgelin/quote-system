package com.stonebridge.quotesystem.common;

import com.stonebridge.quotesystem.business.enums.TrackModuleEnum;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class OssPathUtil {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private OssPathUtil() {
    }

    public static String buildReviewFormDir(String dirPrefix, String orderId) {
        String date = LocalDate.now().format(DATE_FORMATTER);

        if (orderId == null || orderId.trim().isEmpty()) {
            orderId = "temp";
        }

        return clean(dirPrefix) + "/review-form/" + date + "/" + orderId + "/";
    }

    public static String buildLogImageDir(String dirPrefix, String orderId, String moduleType) {
        String date = LocalDate.now().format(DATE_FORMATTER);

        if (orderId == null || orderId.trim().isEmpty()) {
            orderId = "temp";
        }

        TrackModuleEnum moduleEnum = TrackModuleEnum.getByCode(moduleType);
        if (moduleEnum == null) {
            throw new IllegalArgumentException("不支持的模块类型：" + moduleType);
        }

        return clean(dirPrefix) + "/log/" + date + "/" + orderId + "/" + moduleType + "/";
    }

    private static String clean(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "order-tracking";
        }
        return value.replaceAll("^/+", "").replaceAll("/+$", "");
    }
}
