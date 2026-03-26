package com.soulpal.service;

import com.soulpal.exception.RateLimitExceededException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis 기반 Rate Limiter (슬라이딩 윈도우 — 분당 카운터)
 * - 채팅 API: 분당 N건 제한 (userId 키)
 * - 인증 API: 분당 N건 제한 (IP 키) — 브루트포스 방어
 */
@Slf4j
@Service
public class RateLimitService {

    @Value("${rate-limit.chat.capacity:30}")
    private int chatCapacity;

    @Value("${rate-limit.auth.capacity:10}")
    private int authCapacity;

    private final StringRedisTemplate redis;

    public RateLimitService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void checkChat(String userId) {
        check("ratelimit:chat:" + userId, chatCapacity, "chat", "userId=" + userId);
    }

    /** 로그인/회원가입 IP 기반 Rate Limit (분당 {@code authCapacity}건) */
    public void checkAuth(String ip) {
        check("ratelimit:auth:" + ip, authCapacity, "auth", "ip=" + ip);
    }

    private void check(String key, int capacity, String tag, String subject) {
        long bucket = System.currentTimeMillis() / 60_000;
        String bucketKey = key + ":" + bucket;
        Long count = redis.opsForValue().increment(bucketKey);
        if (count == 1) {
            redis.expire(bucketKey, Duration.ofMinutes(2));
        }
        if (count >= capacity * 0.8 && count < capacity) {
            log.warn("[RATE_LIMIT] 한도 근접: tag={}, {}, count={}/{}", tag, subject, count, capacity);
        }
        if (count > capacity) {
            log.warn("[RATE_LIMIT] 한도 초과: tag={}, {}, count={}/{}", tag, subject, count, capacity);
            throw new RateLimitExceededException();
        }
    }
}
