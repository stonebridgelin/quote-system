package com.stonebridge.quotesystem.security.filter;

import com.stonebridge.quotesystem.security.dto.LoginRequest;
import com.stonebridge.quotesystem.security.entity.SecurityUser;
import com.stonebridge.quotesystem.security.service.AuthorizationCacheService;
import com.stonebridge.quotesystem.security.utils.JwtUtil;
import com.stonebridge.quotesystem.security.utils.QuoteSecurityProperties;
import com.stonebridge.quotesystem.security.utils.SecurityResponseWriter;
import com.stonebridge.quotesystem.system.entity.SysUser;
import com.stonebridge.quotesystem.system.mapper.SysUserMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class JwtLoginFilter extends UsernamePasswordAuthenticationFilter {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtUtil jwtUtil;
    private final QuoteSecurityProperties properties;
    private final SysUserMapper sysUserMapper;
    private final AuthorizationCacheService authorizationCacheService;

    public JwtLoginFilter(AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil,
                          QuoteSecurityProperties properties,
                          SysUserMapper sysUserMapper,
                          AuthorizationCacheService authorizationCacheService) {
        this.jwtUtil = jwtUtil;
        this.properties = properties;
        this.sysUserMapper = sysUserMapper;
        this.authorizationCacheService = authorizationCacheService;
        setAuthenticationManager(authenticationManager);
        setFilterProcessesUrl("/auth/login");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
                                                HttpServletResponse response) throws AuthenticationException {
        try {
            LoginRequest loginRequest = objectMapper.readValue(request.getInputStream(), LoginRequest.class);
            String username = loginRequest == null ? null : loginRequest.getUsername();
            String password = loginRequest == null ? null : loginRequest.getPassword();
            if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
                username = request.getParameter("username");
                password = request.getParameter("password");
            }
            UsernamePasswordAuthenticationToken authenticationToken =
                    UsernamePasswordAuthenticationToken.unauthenticated(username, password);
            setDetails(request, authenticationToken);
            return this.getAuthenticationManager().authenticate(authenticationToken);
        } catch (IOException e) {
            throw new RuntimeException("登录请求解析失败", e);
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain,
                                            Authentication authResult) throws IOException {
        SecurityUser securityUser = (SecurityUser) authResult.getPrincipal();
        String accessToken = jwtUtil.generateAccessToken(securityUser);

        updateLoginInfo(request, securityUser);
        authorizationCacheService.cache(securityUser);

        Map<String, Object> userInfo = new HashMap<>();
        SysUser user = securityUser.getSysUser();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("realName", user.getRealName());
        userInfo.put("avatar", user.getAvatar());

        Map<String, Object> data = new HashMap<>();
        data.put("accessToken", accessToken);
        data.put("token", accessToken);
        data.put("tokenType", properties.getJwt().getTokenPrefix().trim());
        data.put("expiresIn", properties.getJwt().getAccessTokenExpireSeconds());
        data.put("userInfo", userInfo);
        data.put("roles", securityUser.getRoles());
        data.put("permissions", securityUser.getPermissions());

        SecurityResponseWriter.writeSuccess(response, "登录成功", data);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request,
                                              HttpServletResponse response,
                                              AuthenticationException failed) throws IOException {
        String message = failed.getMessage();
        if (!StringUtils.hasText(message)) {
            message = "用户名或密码错误";
        }
        SecurityResponseWriter.writeFail(response, HttpServletResponse.SC_UNAUTHORIZED, 401, message);
    }

    private void updateLoginInfo(HttpServletRequest request, SecurityUser securityUser) {
        try {
            String ip = request.getHeader("X-Forwarded-For");
            if (!StringUtils.hasText(ip)) {
                ip = request.getRemoteAddr();
            }
            sysUserMapper.updateLoginInfo(securityUser.getUserId(), LocalDateTime.now(), ip);
        } catch (Exception ignored) {
        }
    }
}
