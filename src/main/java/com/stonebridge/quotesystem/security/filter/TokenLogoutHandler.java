package com.stonebridge.quotesystem.security.filter;

import com.stonebridge.quotesystem.security.utils.JwtUtil;
import com.stonebridge.quotesystem.security.utils.QuoteSecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

public class TokenLogoutHandler implements LogoutHandler {
    private final JwtUtil jwtUtil;
    private final QuoteSecurityProperties properties;
    private final RedisTemplate<String, Object> redisTemplate;

    public TokenLogoutHandler(JwtUtil jwtUtil,
                              QuoteSecurityProperties properties,
                              RedisTemplate<String, Object> redisTemplate) {
        this.jwtUtil = jwtUtil;
        this.properties = properties;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (redisTemplate == null) {
            return;
        }
        String headerValue = request.getHeader(properties.getJwt().getHeaderName());
        if (!StringUtils.hasText(headerValue)) {
            return;
        }
        String rawToken = jwtUtil.stripPrefix(headerValue);
        try {
            long remainingSeconds = jwtUtil.getRemainingSeconds(rawToken);
            if (remainingSeconds > 0) {
                redisTemplate.opsForValue().set(
                        properties.getJwt().getBlacklistKeyPrefix() + rawToken,
                        "1",
                        remainingSeconds,
                        TimeUnit.SECONDS
                );
            }
        } catch (Exception ignored) {
        }
    }
}
