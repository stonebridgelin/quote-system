package com.stonebridge.quotesystem.common;

import lombok.Data;

/**
 * 全局统一响应实体类
 */
@Data
public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    // 私有化构造方法，强制使用静态工厂方法创建对象
    private Result() {}

    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // --- 成功响应 ---
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    // --- 失败响应 ---
    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message, null);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}