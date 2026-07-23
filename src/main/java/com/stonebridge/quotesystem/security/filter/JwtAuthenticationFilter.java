package com.stonebridge.quotesystem.security.filter;

import com.stonebridge.quotesystem.security.utils.JwtUtil;
import com.stonebridge.quotesystem.security.utils.QuoteSecurityProperties;
import com.stonebridge.quotesystem.security.entity.SecurityUser;
import com.stonebridge.quotesystem.security.service.AuthorizationCacheService;
import com.stonebridge.quotesystem.security.service.JwtTokenBlacklistService;
import com.stonebridge.quotesystem.security.utils.SecurityResponseWriter;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final QuoteSecurityProperties properties;
    private final AuthorizationCacheService authorizationCacheService;
    private final JwtTokenBlacklistService tokenBlacklistService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil,
                                   QuoteSecurityProperties properties,
                                   AuthorizationCacheService authorizationCacheService,
                                   JwtTokenBlacklistService tokenBlacklistService) {
        this.jwtUtil = jwtUtil;
        this.properties = properties;
        this.authorizationCacheService = authorizationCacheService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || "/auth/login".equals(path)
                || "/auth/register".equals(path);
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
            Claims claims = jwtUtil.parseClaims(rawToken);
            String jwtId = claims.getId();
            if (!StringUtils.hasText(jwtId)) {
                throw new IllegalArgumentException("Token缺少jti");
            }
            if (tokenBlacklistService.isBlacklisted(jwtId)) {
                filterChain.doFilter(request, response);
                return;
            }
            String username = claims.getSubject();
            String userId = claims.get("userId", String.class);
            if (StringUtils.hasText(username) && SecurityContextHolder.getContext().getAuthentication() == null) {
                SecurityUser userDetails = authorizationCacheService.getOrLoad(userId, username);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (AuthenticationServiceException ex) {
            SecurityContextHolder.clearContext();
            SecurityResponseWriter.writeFail(response,
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE, 503, "认证服务暂时不可用");
            return;
        } catch (AuthenticationException ex) {
            SecurityContextHolder.clearContext();
            SecurityResponseWriter.writeFail(response,
                    HttpServletResponse.SC_UNAUTHORIZED, 401, ex.getMessage());
            return;
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
        }
        filterChain.doFilter(request, response);
    }
}
