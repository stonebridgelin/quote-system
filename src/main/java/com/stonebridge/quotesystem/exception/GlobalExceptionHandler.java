package com.stonebridge.quotesystem.exception;

import com.stonebridge.quotesystem.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. 处理自定义业务异常 (比如主动 throw new BusinessException("库存不足"))
    @ExceptionHandler(BusinessException.class)
    public Result<String> handleBusinessException(BusinessException e) {
        log.info("业务异常: {}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    // 2. 处理表单参数校验异常 (比如 @NotNull 没通过)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<String> handleValidationException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        String errorMsg = bindingResult.getAllErrors().get(0).getDefaultMessage();
        log.warn("参数校验失败: {}", errorMsg);
        return Result.fail(400, errorMsg);
    }

    // 3. 处理数据库唯一索引冲突 (比如重复插入相同的器型代号)
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<String> handleDuplicateKeyException(DuplicateKeyException e) {
        log.error("数据库违反唯一约束", e);
        return Result.fail(400, "操作失败：该数据已存在，请勿重复录入！");
    }

    // 4. 终极兜底：处理所有未知的 Exception (如空指针、除数为0等系统级崩溃)
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        log.error("系统发生未知异常:", e);
        return Result.fail(500, "系统繁忙，请稍后再试！");
    }
}