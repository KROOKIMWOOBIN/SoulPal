package com.soulpal.service;

import com.soulpal.exception.RateLimitExceededException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis 기반 Rate Limiter (슬라이딩 윈도우 — 분당 카운터)
 * - 채팅 API: 분당 N건 제한
 */
@Slf4j
@Service
public class RateLimitService {

    @Value("${rate-limit.chat.capacity:30}")
    private int capacity;

    private final StringRedisTemplate redis;

    public RateLimitService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void checkChat(String userId) {
        long bucket = System.currentTimeMillis() / 60_000;
        String key = "ratelimit:chat:" + userId + ":" + bucket;
        Long count = redis.opsForValue().increment(key);
        if (count == 1) {
            redis.expire(key, Duration.ofMinutes(2));
        }
        if (count >= capacity * 0.8 && count < capacity) {
            log.warn("[RATE_LIMIT] 한도 근접: userId={}, count={}/{}", userId, count, capacity);
        }
        if (count > capacity) {
            log.warn("[RATE_LIMIT] 한도 초과: userId={}, count={}/{}", userId, count, capacity);
            throw new RateLimitExceededException();
        }
    }
}
