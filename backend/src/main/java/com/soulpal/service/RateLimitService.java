package com.soulpal.service;

import com.soulpal.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 사용자별 인메모리 Rate Limiter (Bucket4j Token Bucket)
 * - 채팅 API: 분당 N건 제한
 */
@Service
public class RateLimitService {

    @Value("${rate-limit.chat.capacity:30}")
    private int capacity;

    @Value("${rate-limit.chat.refill-per-minute:20}")
    private int refillPerMinute;

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * 채팅 요청 허용 여부 확인. 초과 시 RateLimitExceededException.
     */
    public void checkChat(String userId) {
        Bucket bucket = buckets.computeIfAbsent(userId, this::newBucket);
        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException();
        }
    }

    private Bucket newBucket(String userId) {
        Bandwidth limit = Bandwidth.classic(
                capacity,
                Refill.intervally(refillPerMinute, Duration.ofMinutes(1))
        );
        return Bucket.builder().addLimit(limit).build();
    }
}
