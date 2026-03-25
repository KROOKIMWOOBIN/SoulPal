package com.soulpal.service;

import com.soulpal.config.JwtUtil;
import com.soulpal.exception.BusinessException;
import com.soulpal.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis 기반 JWT 토큰 관리
 * - 리프레시 토큰 저장 (7일 TTL)
 * - 액세스 토큰 블랙리스트 (잔여 TTL 동안)
 */
@Service
@RequiredArgsConstructor
public class TokenService {

    private final StringRedisTemplate redis;
    private final JwtUtil jwtUtil;

    private static final String REFRESH_PREFIX   = "refresh:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    // ── 리프레시 토큰 ──────────────────────────────────────────────────────────

    public void saveRefreshToken(String userId, String refreshToken) {
        redis.opsForValue().set(
            REFRESH_PREFIX + userId,
            refreshToken,
            Duration.ofMillis(jwtUtil.getRefreshExpiration())
        );
    }

    public String getRefreshToken(String userId) {
        return redis.opsForValue().get(REFRESH_PREFIX + userId);
    }

    public void deleteRefreshToken(String userId) {
        redis.delete(REFRESH_PREFIX + userId);
    }

    /** 리프레시 토큰으로 새 액세스 토큰 발급 */
    public String refresh(String refreshToken) {
        Claims claims;
        try {
            claims = jwtUtil.parse(refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        if (!"refresh".equals(claims.get("type", String.class))) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        String userId   = claims.getSubject();
        String username = claims.get("username", String.class);
        String stored   = getRefreshToken(userId);

        if (!refreshToken.equals(stored)) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다. 다시 로그인해주세요.");
        }

        return jwtUtil.generateAccessToken(userId, username);
    }

    // ── 액세스 토큰 블랙리스트 ────────────────────────────────────────────────

    public void blacklist(String accessToken) {
        try {
            Claims claims = jwtUtil.parse(accessToken);
            long remainingMs = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (remainingMs > 0) {
                redis.opsForValue().set(
                    BLACKLIST_PREFIX + accessToken,
                    "1",
                    Duration.ofMillis(remainingMs)
                );
            }
        } catch (Exception ignored) {
            // 이미 만료된 토큰은 블랙리스트 불필요
        }
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redis.hasKey(BLACKLIST_PREFIX + token));
    }

    // ── 로그아웃 (리프레시 삭제 + 액세스 블랙리스트) ──────────────────────────

    public void logout(String userId, String accessToken) {
        deleteRefreshToken(userId);
        blacklist(accessToken);
    }
}
