package com.stonebridge.quotesystem.security;

import com.stonebridge.quotesystem.security.filter.AccessDeniedHandlerImpl;
import com.stonebridge.quotesystem.security.filter.AuthenticationEntryPointImpl;
import com.stonebridge.quotesystem.security.filter.JwtAuthenticationFilter;
import com.stonebridge.quotesystem.security.filter.JwtLoginFilter;
import com.stonebridge.quotesystem.security.filter.TokenLogoutHandler;
import com.stonebridge.quotesystem.security.utils.JwtUtil;
import com.stonebridge.quotesystem.security.utils.QuoteSecurityProperties;
import com.stonebridge.quotesystem.security.utils.SecurityResponseWriter;
import com.stonebridge.quotesystem.system.mapper.SysUserMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@EnableConfigurationProperties(QuoteSecurityProperties.class)
public class SecurityConfig {
    private final AuthenticationEntryPointImpl authenticationEntryPoint;
    private final AccessDeniedHandlerImpl accessDeniedHandler;
    private final QuoteSecurityProperties properties;

    public SecurityConfig(AuthenticationEntryPointImpl authenticationEntryPoint,
                          AccessDeniedHandlerImpl accessDeniedHandler,
                          QuoteSecurityProperties properties) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.properties = properties;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public AuthenticationProvider daoAuthenticationProvider(UserDetailsService userDetailsService,
                                                            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        provider.setHideUserNotFoundExceptions(false);
        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthenticationManager authenticationManager,
                                                   AuthenticationProvider daoAuthenticationProvider,
                                                   JwtUtil jwtUtil,
                                                   UserDetailsService userDetailsService,
                                                   SysUserMapper sysUserMapper,
                                                   CorsConfigurationSource corsConfigurationSource,
                                                   ObjectProvider<RedisTemplate<String, Object>> redisTemplateProvider) throws Exception {
        RedisTemplate<String, Object> redisTemplate = redisTemplateProvider.getIfAvailable();

        http
                // 前后端分离项目：显式使用当前 CorsConfigurationSource，不改变既有跨域范围。
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                // JWT 通过请求头传递，不依赖 Cookie Session，因此关闭 CSRF 与请求缓存。
                .csrf(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                // 彻底禁用服务端 Session，所有请求均由 JWT 独立完成认证。
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 401/403 由自定义处理器转交 GlobalExceptionHandler，保持统一 Result JSON。
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authenticationProvider(daoAuthenticationProvider);

        if (Boolean.FALSE.equals(properties.getAuthEnabled())) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }

        JwtLoginFilter jwtLoginFilter = new JwtLoginFilter(authenticationManager, jwtUtil, properties, sysUserMapper);
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtUtil, properties, userDetailsService, redisTemplate);

        http.authorizeHttpRequests(auth -> auth
                        // 放行所有 CORS 预检请求，避免 OPTIONS 在 JWT 校验前被拒绝。
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/register").permitAll()
                        .requestMatchers(
                                "/auth/register",
                                "/druid/**", "/druid-system/**", "/druid-business/**",
                                "/swagger-ui.html", "/swagger-ui/**", "/webjars/**",
                                "/v3/api-docs/**", "/doc.html",
                                "/favicon.ico", "/static/**", "/public/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .addLogoutHandler(new TokenLogoutHandler(jwtUtil, properties, redisTemplate))
                        .logoutSuccessHandler(jsonLogoutSuccessHandler())
                )
                .addFilterAt(jwtLoginFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public LogoutSuccessHandler jsonLogoutSuccessHandler() {
        return (request, response, authentication) ->
                SecurityResponseWriter.writeSuccess(response, "退出登录成功", null);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 保持原有规则：允许任意来源模式、请求头和方法，并允许携带凭证。
        configuration.addAllowedOriginPattern("*");
        configuration.addAllowedHeader("*");
        // "*" 已包含 OPTIONS，配合过滤器链中的显式 permitAll 确保预检请求通过。
        configuration.addAllowedMethod("*");
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
