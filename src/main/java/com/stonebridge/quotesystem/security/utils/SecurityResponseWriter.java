package com.stonebridge.quotesystem.security.utils;

import com.stonebridge.quotesystem.common.Result;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class SecurityResponseWriter {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private SecurityResponseWriter() {
    }

    public static void writeFail(HttpServletResponse response, int httpStatus, int code, String message) throws IOException {
        write(response, httpStatus, Result.fail(code, message));
    }

    public static void writeSuccess(HttpServletResponse response, String message, Object data) throws IOException {
        write(response, HttpServletResponse.SC_OK, Result.success(message, data));
    }

    public static void write(HttpServletResponse response, int httpStatus, Result<?> result) throws IOException {
        response.setStatus(httpStatus);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(result));
    }
}
