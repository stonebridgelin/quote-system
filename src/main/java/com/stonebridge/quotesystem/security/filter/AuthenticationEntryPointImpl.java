package com.stonebridge.quotesystem.security.filter;

import com.stonebridge.quotesystem.exception.BusinessException;
import com.stonebridge.quotesystem.security.utils.SecurityResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;

@Component
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {
    private static final String UNAUTHORIZED_MESSAGE = "未登录或登录已过期";

    private final HandlerExceptionResolver exceptionResolver;

    public AuthenticationEntryPointImpl(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        // Security 异常发生在 MVC Controller 之前，需要主动交给全局异常解析器处理。
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        ModelAndView resolved = exceptionResolver.resolveException(
                request,
                response,
                null,
                new BusinessException(HttpServletResponse.SC_UNAUTHORIZED, UNAUTHORIZED_MESSAGE)
        );

        // 理论上 BusinessException 会被 GlobalExceptionHandler 接管；保留兜底以确保响应结构稳定。
        if (resolved == null && !response.isCommitted()) {
            SecurityResponseWriter.writeFail(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    UNAUTHORIZED_MESSAGE
            );
        }
    }
}
