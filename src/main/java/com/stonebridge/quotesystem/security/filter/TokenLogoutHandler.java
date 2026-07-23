package com.stonebridge.quotesystem.security.filter;

import com.stonebridge.quotesystem.security.utils.JwtUtil;
import com.stonebridge.quotesystem.security.utils.QuoteSecurityProperties;
import com.stonebridge.quotesystem.security.service.JwtTokenBlacklistService;
import com.stonebridge.quotesystem.security.utils.SecurityResponseWriter;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.util.StringUtils;

import java.io.IOException;

public class TokenLogoutHandler implements LogoutHandler {
    public static final String LOGOUT_FAILURE_ATTRIBUTE =
            TokenLogoutHandler.class.getName() + ".LOGOUT_FAILURE";

    private final JwtUtil jwtUtil;
    private final QuoteSecurityProperties properties;
    private final JwtTokenBlacklistService tokenBlacklistService;

    public TokenLogoutHandler(JwtUtil jwtUtil,
                              QuoteSecurityProperties properties,
                              JwtTokenBlacklistService tokenBlacklistService) {
        this.jwtUtil = jwtUtil;
        this.properties = properties;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        String headerValue = request.getHeader(properties.getJwt().getHeaderName());
        if (!StringUtils.hasText(headerValue)) {
            return;
        }
        String rawToken = jwtUtil.stripPrefix(headerValue);
        try {
            String jwtId = jwtUtil.getJwtId(rawToken);
            if (!StringUtils.hasText(jwtId)) {
                writeFailure(request, response, HttpServletResponse.SC_UNAUTHORIZED, 401, "无效的Token");
                return;
            }
            long remainingSeconds = jwtUtil.getRemainingSeconds(rawToken);
            if (remainingSeconds > 0) {
                tokenBlacklistService.blacklist(jwtId, remainingSeconds);
            }
        } catch (AuthenticationServiceException exception) {
            writeFailure(request, response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    503, "认证服务暂时不可用");
        } catch (JwtException | IllegalArgumentException exception) {
            writeFailure(request, response, HttpServletResponse.SC_UNAUTHORIZED, 401, "无效的Token");
        }
    }

    private void writeFailure(HttpServletRequest request, HttpServletResponse response,
                              int httpStatus, int code, String message) {
        request.setAttribute(LOGOUT_FAILURE_ATTRIBUTE, Boolean.TRUE);
        try {
            SecurityResponseWriter.writeFail(response, httpStatus, code, message);
        } catch (IOException exception) {
            throw new IllegalStateException("写入退出登录失败响应异常", exception);
        }
    }
}
