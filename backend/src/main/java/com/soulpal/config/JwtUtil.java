package com.soulpal.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret:soulpal-default-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm}")
    private String secret;

    @Value("${jwt.access-expiration:3600000}")      // 1시간
    private long accessExpiration;

    @Value("${jwt.refresh-expiration:604800000}")   // 7일
    private long refreshExpiration;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateAccessToken(String userId, String username) {
        return Jwts.builder()
                .subject(userId)
                .claim("username", username)
                .claim("type", "access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpiration))
                .signWith(key())
                .compact();
    }

    public String generateRefreshToken(String userId, String username) {
        return Jwts.builder()
                .subject(userId)
                .claim("username", username)
                .claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(key())
                .compact();
    }

    // 하위 호환
    public String generate(String userId, String username) {
        return generateAccessToken(userId, username);
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public long getAccessExpiration() { return accessExpiration; }
    public long getRefreshExpiration() { return refreshExpiration; }
}
