package com.atp.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 令牌签发与解析
 *
 * <p>载荷：userId / username(subject) / role
 */
@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${atp.jwt.secret}")
    private String secret;

    @Value("${atp.jwt.expiration}")
    private Long expiration;

    private SecretKey key;

    @PostConstruct
    public void init() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT 密钥长度不足 32 字节，无法使用 HS256 算法");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 签发令牌
     */
    public String generateToken(Long userId, String username, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /**
     * 解析令牌，失败抛出 JwtException
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 令牌是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT 校验失败：{}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取令牌剩余有效毫秒数（用于登出黑名单 TTL）
     */
    public long getRemainingMillis(Claims claims) {
        Date expiration = claims.getExpiration();
        if (expiration == null) {
            return this.expiration;
        }
        long remaining = expiration.getTime() - System.currentTimeMillis();
        return Math.max(remaining, 0);
    }

    public Long getExpiration() {
        return expiration;
    }
}
