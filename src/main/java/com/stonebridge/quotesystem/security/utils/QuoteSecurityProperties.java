package com.stonebridge.quotesystem.security.utils;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "security")
public class QuoteSecurityProperties {
    private Boolean authEnabled = true;
    private Jwt jwt = new Jwt();
    private AuthorizationCache authorizationCache = new AuthorizationCache();

    @Data
    public static class Jwt {
        /** HS256 密钥，长度建议不少于 32 字节。 */
        private String secret = "QuoteSystemJwtSecretKeyQuoteSystemJwtSecretKey2026QuoteSystemJwtSecretKey";
        private String issuer = "quote-system";
        private String headerName = "Authorization";
        private String tokenPrefix = "Bearer ";
        private Long accessTokenExpireSeconds = 7200L;
        private String blacklistKeyPrefix = "security:jwt:blacklist:";
    }

    @Data
    public static class AuthorizationCache {
        private String keyPrefix = "security:authorization:user:";
        private Long ttlSeconds = 600L;
    }
}
