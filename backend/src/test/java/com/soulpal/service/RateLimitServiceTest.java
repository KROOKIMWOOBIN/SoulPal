package com.soulpal.service;

import com.soulpal.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitService 테스트")
class RateLimitServiceTest {

    @Mock StringRedisTemplate  redis;
    @Mock ValueOperations<String, String> valueOps;

    @InjectMocks RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        given(redis.opsForValue()).willReturn(valueOps);
        ReflectionTestUtils.setField(rateLimitService, "chatCapacity", 3);
        ReflectionTestUtils.setField(rateLimitService, "authCapacity", 3);
    }

    @Test
    @DisplayName("허용 범위 내 요청 → 예외 없음")
    void checkChat_withinLimit() {
        given(valueOps.increment(anyString())).willReturn(1L, 2L, 3L);

        assertThatNoException().isThrownBy(() -> {
            rateLimitService.checkChat("user-1");
            rateLimitService.checkChat("user-1");
            rateLimitService.checkChat("user-1");
        });
    }

    @Test
    @DisplayName("허용 초과 요청 → RateLimitExceededException")
    void checkChat_exceeded() {
        given(valueOps.increment(anyString())).willReturn(4L);

        assertThatThrownBy(() -> rateLimitService.checkChat("user-2"))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    @DisplayName("80% 근접 시 경고 로그 — 예외 없이 통과")
    void checkChat_nearLimit_noException() {
        // capacity=5로 임시 변경: 80%=4, count=4 → near limit
        ReflectionTestUtils.setField(rateLimitService, "chatCapacity", 5);
        given(valueOps.increment(anyString())).willReturn(4L);

        assertThatNoException().isThrownBy(() -> rateLimitService.checkChat("user-3"));
    }

    @Test
    @DisplayName("첫 요청이면 TTL 설정 (count==1)")
    void checkChat_firstRequest_setsTtl() {
        given(valueOps.increment(anyString())).willReturn(1L);

        rateLimitService.checkChat("user-4");

        then(redis).should().expire(anyString(), any());
    }

    @Test
    @DisplayName("두 번째 이후 요청은 TTL 재설정 안 함")
    void checkChat_subsequentRequest_noTtlReset() {
        given(valueOps.increment(anyString())).willReturn(2L);

        rateLimitService.checkChat("user-5");

        then(redis).should(never()).expire(anyString(), any());
    }

    // ── Auth Rate Limit ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Auth: 허용 범위 내 요청 → 예외 없음")
    void checkAuth_withinLimit() {
        given(valueOps.increment(anyString())).willReturn(1L, 2L, 3L);

        assertThatNoException().isThrownBy(() -> {
            rateLimitService.checkAuth("1.2.3.4");
            rateLimitService.checkAuth("1.2.3.4");
            rateLimitService.checkAuth("1.2.3.4");
        });
    }

    @Test
    @DisplayName("Auth: 허용 초과 요청 → RateLimitExceededException")
    void checkAuth_exceeded() {
        given(valueOps.increment(anyString())).willReturn(4L);

        assertThatThrownBy(() -> rateLimitService.checkAuth("1.2.3.4"))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    @DisplayName("Auth: 첫 요청이면 TTL 설정 (count==1)")
    void checkAuth_firstRequest_setsTtl() {
        given(valueOps.increment(anyString())).willReturn(1L);

        rateLimitService.checkAuth("1.2.3.4");

        then(redis).should().expire(anyString(), any());
    }

    @Test
    @DisplayName("Auth: IP별 독립 버킷 — 다른 IP는 별도 카운트")
    void checkAuth_differentIp_independentBuckets() {
        given(valueOps.increment(anyString())).willReturn(4L, 1L);

        // 첫 번째 IP는 초과
        assertThatThrownBy(() -> rateLimitService.checkAuth("1.2.3.4"))
                .isInstanceOf(RateLimitExceededException.class);

        // 두 번째 IP는 정상
        assertThatNoException().isThrownBy(() -> rateLimitService.checkAuth("5.6.7.8"));
    }
}
