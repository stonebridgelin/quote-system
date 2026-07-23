package com.stonebridge.quotesystem.security.filter;

import com.stonebridge.quotesystem.exception.BusinessException;
import com.stonebridge.quotesystem.security.utils.SecurityResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;

@Component
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {
    private static final String FORBIDDEN_MESSAGE = "当前账号无权限访问该功能";

    private final HandlerExceptionResolver exceptionResolver;

    public AccessDeniedHandlerImpl(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        // 将过滤器链中的 403 统一交给 GlobalExceptionHandler 生成 Result JSON。
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        ModelAndView resolved = exceptionResolver.resolveException(
                request,
                response,
                null,
                new BusinessException(HttpServletResponse.SC_FORBIDDEN, FORBIDDEN_MESSAGE)
        );

        // 全局解析器不可用时仍返回原有 code/message/data 结构，避免前端解析失败。
        if (resolved == null && !response.isCommitted()) {
            SecurityResponseWriter.writeFail(
                    response,
                    HttpServletResponse.SC_FORBIDDEN,
                    HttpServletResponse.SC_FORBIDDEN,
                    FORBIDDEN_MESSAGE
            );
        }
    }
}
