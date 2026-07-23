package com.stonebridge.quotesystem.security.utils;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    /** 建议至少 32 位以上。 */
    private String secretKey = "StoneBridgeQuoteSystemSecretKey2026StoneBridgeQuoteSystemSecretKey2026";
    /** 过期时间，单位毫秒。默认 24 小时。 */
    private long expirationTime = 24 * 60 * 60 * 1000L;
    private String header = "Authorization";
    private String tokenPrefix = "Bearer ";
}
