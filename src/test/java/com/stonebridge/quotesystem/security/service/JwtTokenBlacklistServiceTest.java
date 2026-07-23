package com.stonebridge.quotesystem.security.service;

import com.stonebridge.quotesystem.security.utils.QuoteSecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationServiceException;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenBlacklistServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private JwtTokenBlacklistService blacklistService;

    @BeforeEach
    void setUp() {
        QuoteSecurityProperties properties = new QuoteSecurityProperties();
        properties.getJwt().setBlacklistKeyPrefix("security:jwt:blacklist:");
        blacklistService = new JwtTokenBlacklistService(redisTemplate, properties);
    }

    @Test
    void shouldStoreOnlyJwtIdWithRemainingTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        blacklistService.blacklist("296ed631-67f6-400e-8440-47c89dabf090", 120L);

        verify(valueOperations).set(
                "security:jwt:blacklist:296ed631-67f6-400e-8440-47c89dabf090",
                "1", 120L, TimeUnit.SECONDS);
    }

    @Test
    void shouldFailClosedWhenRedisCannotCheckBlacklist() {
        when(redisTemplate.hasKey("security:jwt:blacklist:jti-1"))
                .thenThrow(new IllegalStateException("redis unavailable"));

        AuthenticationServiceException exception = assertThrows(
                AuthenticationServiceException.class,
                () -> blacklistService.isBlacklisted("jti-1"));

        assertEquals("Token状态服务暂时不可用", exception.getMessage());
        assertTrue(exception.getCause() instanceof IllegalStateException);
    }
}
