package com.stonebridge.quotesystem.security.filter;

import com.stonebridge.quotesystem.security.utils.JwtUtil;
import com.stonebridge.quotesystem.security.utils.QuoteSecurityProperties;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final QuoteSecurityProperties properties;
    private final UserDetailsService userDetailsService;
    private final RedisTemplate<String, Object> redisTemplate;

    public JwtAuthenticationFilter(JwtUtil jwtUtil,
                                   QuoteSecurityProperties properties,
                                   UserDetailsService userDetailsService,
                                   RedisTemplate<String, Object> redisTemplate) {
        this.jwtUtil = jwtUtil;
        this.properties = properties;
        this.userDetailsService = userDetailsService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String headerValue = request.getHeader(properties.getJwt().getHeaderName());
        if (!StringUtils.hasText(headerValue)) {
            filterChain.doFilter(request, response);
            return;
        }

        String rawToken = jwtUtil.stripPrefix(headerValue);
        try {
            if (isBlacklisted(rawToken)) {
                filterChain.doFilter(request, response);
                return;
            }
            String username = jwtUtil.getUsername(rawToken);
            if (StringUtils.hasText(username) && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
        }
        filterChain.doFilter(request, response);
    }

    private boolean isBlacklisted(String rawToken) {
        if (redisTemplate == null || !StringUtils.hasText(rawToken)) {
            return false;
        }
        try {
            Boolean exists = redisTemplate.hasKey(properties.getJwt().getBlacklistKeyPrefix() + rawToken);
            return Boolean.TRUE.equals(exists);
        } catch (Exception ignored) {
            return false;
        }
    }
}
