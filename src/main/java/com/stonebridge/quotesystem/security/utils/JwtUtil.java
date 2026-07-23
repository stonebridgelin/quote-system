package com.stonebridge.quotesystem.security.utils;

import com.stonebridge.quotesystem.security.entity.SecurityUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {
    private final QuoteSecurityProperties properties;
    private final SecretKey secretKey;

    public JwtUtil(QuoteSecurityProperties properties) {
        this.properties = properties;
        this.secretKey = Keys.hmacShaKeyFor(properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(SecurityUser user) {
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(properties.getJwt().getAccessTokenExpireSeconds());
        return Jwts.builder()
                .issuer(properties.getJwt().getIssuer())
                .subject(user.getUsername())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .claim("userId", user.getUserId())
                .claim("username", user.getUsername())
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(properties.getJwt().getIssuer())
                .build()
                .parseSignedClaims(stripPrefix(token))
                .getPayload();
    }

    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public String getUserId(String token) {
        return parseClaims(token).get("userId", String.class);
    }

    public String getJwtId(String token) {
        return parseClaims(token).getId();
    }

    public Date getExpiration(String token) {
        return parseClaims(token).getExpiration();
    }

    public long getRemainingSeconds(String token) {
        Date expiration = getExpiration(token);
        long remainingMillis = expiration.getTime() - System.currentTimeMillis();
        return Math.max(0, remainingMillis / 1000);
    }

    public String stripPrefix(String token) {
        if (!StringUtils.hasText(token)) {
            return token;
        }
        String prefix = properties.getJwt().getTokenPrefix();
        if (StringUtils.hasText(prefix) && token.startsWith(prefix)) {
            return token.substring(prefix.length()).trim();
        }
        return token.trim();
    }
}
