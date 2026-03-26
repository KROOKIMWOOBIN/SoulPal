package com.soulpal.service;

import com.soulpal.config.JwtUtil;
import com.soulpal.exception.BusinessException;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TokenService 테스트")
class TokenServiceTest {

    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> valueOps;
    @Mock JwtUtil jwtUtil;
    @Mock Claims claims;

    @InjectMocks TokenService tokenService;

    @BeforeEach
    void setUp() {
        given(redis.opsForValue()).willReturn(valueOps);
    }

    // ── saveRefreshToken ──────────────────────────────────────────────────────

    @Test
    @DisplayName("리프레시 토큰 저장 — Redis에 TTL과 함께 저장")
    void saveRefreshToken_storesInRedisWithTtl() {
        given(jwtUtil.getRefreshExpiration()).willReturn(604800000L);

        tokenService.saveRefreshToken("user-1", "refresh-token");

        then(valueOps).should().set(eq("refresh:user-1"), eq("refresh-token"), any());
    }

    // ── getRefreshToken ───────────────────────────────────────────────────────

    @Test
    @DisplayName("리프레시 토큰 조회 — Redis에서 반환")
    void getRefreshToken_returnsStoredToken() {
        given(valueOps.get("refresh:user-1")).willReturn("refresh-token");

        String token = tokenService.getRefreshToken("user-1");

        assertThat(token).isEqualTo("refresh-token");
    }

    // ── refresh ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("토큰 갱신 — 유효한 리프레시 토큰으로 새 액세스+리프레시 토큰 발급 (rotation)")
    void refresh_validToken_returnsNewTokens() {
        given(jwtUtil.parse("refresh-token")).willReturn(claims);
        given(claims.get("type", String.class)).willReturn("refresh");
        given(claims.getSubject()).willReturn("user-1");
        given(claims.get("username", String.class)).willReturn("testuser");
        given(valueOps.get("refresh:user-1")).willReturn("refresh-token");
        given(jwtUtil.generateAccessToken("user-1", "testuser")).willReturn("new-access-token");
        given(jwtUtil.generateRefreshToken("user-1", "testuser")).willReturn("new-refresh-token");
        given(jwtUtil.getRefreshExpiration()).willReturn(604800000L);

        Map<String, String> result = tokenService.refresh("refresh-token");

        assertThat(result.get("accessToken")).isEqualTo("new-access-token");
        assertThat(result.get("refreshToken")).isEqualTo("new-refresh-token");
        then(redis).should().delete("refresh:user-1"); // old token deleted
        then(valueOps).should().set(eq("refresh:user-1"), eq("new-refresh-token"), any()); // new token saved
    }

    @Test
    @DisplayName("토큰 갱신 — 타입이 refresh가 아닌 경우 BusinessException")
    void refresh_wrongType_throwsException() {
        given(jwtUtil.parse("access-token")).willReturn(claims);
        given(claims.get("type", String.class)).willReturn("access");

        assertThatThrownBy(() -> tokenService.refresh("access-token"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("토큰 갱신 — 저장된 토큰과 불일치 시 BusinessException")
    void refresh_tokenMismatch_throwsException() {
        given(jwtUtil.parse("other-token")).willReturn(claims);
        given(claims.get("type", String.class)).willReturn("refresh");
        given(claims.getSubject()).willReturn("user-1");
        given(valueOps.get("refresh:user-1")).willReturn("stored-token");

        assertThatThrownBy(() -> tokenService.refresh("other-token"))
                .isInstanceOf(BusinessException.class);
    }

    // ── blacklist / isBlacklisted ─────────────────────────────────────────────

    @Test
    @DisplayName("블랙리스트 추가 — 잔여 TTL로 Redis에 저장")
    void blacklist_validToken_storesInRedis() {
        long future = System.currentTimeMillis() + 60_000;
        given(jwtUtil.parse("access-token")).willReturn(claims);
        given(claims.getExpiration()).willReturn(new Date(future));

        tokenService.blacklist("access-token");

        then(valueOps).should().set(eq("blacklist:access-token"), eq("1"), any());
    }

    @Test
    @DisplayName("블랙리스트 확인 — 등록된 토큰은 true 반환")
    void isBlacklisted_blacklistedToken_returnsTrue() {
        given(redis.hasKey("blacklist:access-token")).willReturn(true);

        assertThat(tokenService.isBlacklisted("access-token")).isTrue();
    }

    @Test
    @DisplayName("블랙리스트 확인 — 미등록 토큰은 false 반환")
    void isBlacklisted_unknownToken_returnsFalse() {
        given(redis.hasKey("blacklist:unknown-token")).willReturn(false);

        assertThat(tokenService.isBlacklisted("unknown-token")).isFalse();
    }

    // ── logout ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("로그아웃 — 리프레시 삭제 + 액세스 블랙리스트")
    void logout_deletesRefreshAndBlacklists() {
        long future = System.currentTimeMillis() + 60_000;
        given(jwtUtil.parse("access-token")).willReturn(claims);
        given(claims.getExpiration()).willReturn(new Date(future));

        tokenService.logout("user-1", "access-token");

        then(redis).should().delete("refresh:user-1");
        then(valueOps).should().set(eq("blacklist:access-token"), eq("1"), any());
    }
}
