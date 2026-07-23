package com.stonebridge.quotesystem.security.service;

import com.stonebridge.quotesystem.security.utils.QuoteSecurityProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/** JWT 主动失效服务。Redis Key 只保存 jti，不保存完整 Token。 */
@Service
public class JwtTokenBlacklistService {

    private final StringRedisTemplate stringRedisTemplate;
    private final QuoteSecurityProperties properties;

    public JwtTokenBlacklistService(StringRedisTemplate stringRedisTemplate,
                                    QuoteSecurityProperties properties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.properties = properties;
    }

    public boolean isBlacklisted(String jwtId) {
        if (!StringUtils.hasText(jwtId)) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(buildKey(jwtId)));
        } catch (RuntimeException exception) {
            // 黑名单不可用时禁止静默放行已注销 Token。
            throw new AuthenticationServiceException("Token状态服务暂时不可用", exception);
        }
    }

    public void blacklist(String jwtId, long remainingSeconds) {
        if (!StringUtils.hasText(jwtId) || remainingSeconds <= 0) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(
                    buildKey(jwtId), "1", remainingSeconds, TimeUnit.SECONDS);
        } catch (RuntimeException exception) {
            throw new AuthenticationServiceException("Token状态服务暂时不可用", exception);
        }
    }

    String buildKey(String jwtId) {
        return properties.getJwt().getBlacklistKeyPrefix() + jwtId;
    }
}
