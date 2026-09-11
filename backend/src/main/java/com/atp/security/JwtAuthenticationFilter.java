package com.atp.security;

import com.atp.common.result.ResultCode;
import com.atp.config.CorsConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * JWT 认证过滤器：解析请求头令牌，构建认证上下文
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BLACKLIST_KEY_PREFIX = "atp:token:blacklist:";

    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);

        if (StringUtils.hasText(token)) {
            try {
                if (isBlacklisted(token)) {
                    writeUnauthorized(response, "登录已失效，请重新登录");
                    return;
                }

                Claims claims = jwtTokenProvider.parseToken(token);
                String username = claims.getSubject();
                Long userId = claims.get("userId", Long.class);
                String role = claims.get("role", String.class);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserPrincipal principal = new UserPrincipal(userId, username, role);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException | IllegalArgumentException e) {
                log.debug("令牌解析失败 uri={} msg={}", request.getRequestURI(), e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从 Authorization 头提取令牌
     */
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    /**
     * 令牌是否已被登出拉黑。Redis 不可用时降级放行，避免中间件故障导致全站不可用
     */
    private boolean isBlacklisted(String token) {
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(BLACKLIST_KEY_PREFIX + token));
        } catch (Exception e) {
            log.warn("Redis 不可用，跳过令牌黑名单校验：{}", e.getMessage());
            return false;
        }
    }

    static void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format(
                "{\"code\":%d,\"message\":\"%s\",\"timestamp\":%d}",
                ResultCode.UNAUTHORIZED.getCode(), message, System.currentTimeMillis()));
    }

    /**
     * 登出时调用：将令牌加入黑名单，TTL 为其剩余有效期
     */
    public void blacklist(String token, long remainingMillis) {
        try {
            if (remainingMillis > 0) {
                stringRedisTemplate.opsForValue().set(
                        BLACKLIST_KEY_PREFIX + token, "1", java.time.Duration.ofMillis(remainingMillis));
            }
        } catch (Exception e) {
            log.warn("Redis 不可用，令牌未能加入黑名单：{}", e.getMessage());
        }
    }

    /**
     * 允许跨域预检请求直接通过
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return CorsConfig.OPTIONS_METHOD.equalsIgnoreCase(request.getMethod());
    }
}
