package com.soulpal.service;

import com.soulpal.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

@DisplayName("RateLimitService 테스트")
class RateLimitServiceTest {

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService();
        // 테스트용: capacity=3, refill=3 (분당 3건)
        ReflectionTestUtils.setField(rateLimitService, "capacity", 3);
        ReflectionTestUtils.setField(rateLimitService, "refillPerMinute", 3);
    }

    @Test
    @DisplayName("허용 범위 내 요청 → 예외 없음")
    void checkChat_withinLimit() {
        assertThatNoException().isThrownBy(() -> {
            rateLimitService.checkChat("user-1");
            rateLimitService.checkChat("user-1");
            rateLimitService.checkChat("user-1");
        });
    }

    @Test
    @DisplayName("허용 초과 요청 → RateLimitExceededException")
    void checkChat_exceeded() {
        // capacity=3 소진
        rateLimitService.checkChat("user-2");
        rateLimitService.checkChat("user-2");
        rateLimitService.checkChat("user-2");

        assertThatThrownBy(() -> rateLimitService.checkChat("user-2"))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    @DisplayName("다른 사용자는 독립적인 버킷 사용")
    void checkChat_separateBuckets() {
        // user-A가 버킷 소진
        rateLimitService.checkChat("user-A");
        rateLimitService.checkChat("user-A");
        rateLimitService.checkChat("user-A");

        // user-B는 영향 없음
        assertThatNoException().isThrownBy(() -> rateLimitService.checkChat("user-B"));
    }
}
