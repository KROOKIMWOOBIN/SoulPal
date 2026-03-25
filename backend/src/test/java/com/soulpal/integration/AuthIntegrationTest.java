package com.soulpal.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soulpal.dto.AuthResponse;
import com.soulpal.dto.LoginRequest;
import com.soulpal.dto.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Auth API 통합 테스트.
 * 실제 PostgreSQL + Redis 컨테이너로 회원가입 → 로그인 → 토큰 갱신 → 로그아웃 플로우를 검증합니다.
 */
@DisplayName("Auth 통합 테스트")
class AuthIntegrationTest extends IntegrationTestBase {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("회원가입 → 로그인 → /me 조회 → 로그아웃 전체 플로우")
    void fullAuthFlow() {
        // 1. 회원가입
        RegisterRequest regReq = new RegisterRequest();
        regReq.setUsername("integrationUser");
        regReq.setEmail("integration@test.com");
        regReq.setPassword("Password123!");

        ResponseEntity<AuthResponse> regResp = restTemplate.postForEntity(
                "/api/auth/register", regReq, AuthResponse.class);

        assertThat(regResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        AuthResponse regBody = regResp.getBody();
        assertThat(regBody).isNotNull();
        assertThat(regBody.getAccessToken()).isNotBlank();
        assertThat(regBody.getRefreshToken()).isNotBlank();
        assertThat(regBody.getUsername()).isEqualTo("integrationUser");

        String accessToken  = regBody.getAccessToken();
        String refreshToken = regBody.getRefreshToken();

        // 2. /me 조회 (인증 필요)
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        ResponseEntity<Map> meResp = restTemplate.exchange(
                "/api/auth/me", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        assertThat(meResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meResp.getBody()).containsKey("userId");
        assertThat(meResp.getBody().get("username")).isEqualTo("integrationUser");

        // 3. 토큰 갱신
        ResponseEntity<Map> refreshResp = restTemplate.postForEntity(
                "/api/auth/refresh",
                Map.of("refreshToken", refreshToken),
                Map.class);
        assertThat(refreshResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        String newAccessToken = (String) refreshResp.getBody().get("accessToken");
        assertThat(newAccessToken).isNotBlank();

        // 4. 로그아웃
        HttpHeaders logoutHeaders = new HttpHeaders();
        logoutHeaders.setBearerAuth(newAccessToken);

        ResponseEntity<Void> logoutResp = restTemplate.exchange(
                "/api/auth/logout", HttpMethod.POST,
                new HttpEntity<>(logoutHeaders), Void.class);
        assertThat(logoutResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // 5. 로그아웃 후 블랙리스트된 토큰으로 /me 요청 → 401
        HttpHeaders blacklistedHeaders = new HttpHeaders();
        blacklistedHeaders.setBearerAuth(newAccessToken);

        ResponseEntity<Map> afterLogout = restTemplate.exchange(
                "/api/auth/me", HttpMethod.GET,
                new HttpEntity<>(blacklistedHeaders), Map.class);
        assertThat(afterLogout.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("이메일 중복 회원가입 → 400")
    void register_duplicateEmail() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("user1");
        req.setEmail("dup@test.com");
        req.setPassword("Password123!");
        restTemplate.postForEntity("/api/auth/register", req, AuthResponse.class);

        // 같은 이메일로 재시도
        RegisterRequest req2 = new RegisterRequest();
        req2.setUsername("user2");
        req2.setEmail("dup@test.com");
        req2.setPassword("Password123!");

        ResponseEntity<Map> resp = restTemplate.postForEntity(
                "/api/auth/register", req2, Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("잘못된 비밀번호 로그인 → 400")
    void login_wrongPassword() {
        // 먼저 가입
        RegisterRequest regReq = new RegisterRequest();
        regReq.setUsername("loginTest");
        regReq.setEmail("logintest@test.com");
        regReq.setPassword("CorrectPass123!");
        restTemplate.postForEntity("/api/auth/register", regReq, AuthResponse.class);

        // 틀린 비밀번호로 로그인
        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail("logintest@test.com");
        loginReq.setPassword("WrongPass!");

        ResponseEntity<Map> resp = restTemplate.postForEntity(
                "/api/auth/login", loginReq, Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("유효하지 않은 리프레시 토큰 → 401")
    void refresh_invalidToken() {
        ResponseEntity<Map> resp = restTemplate.postForEntity(
                "/api/auth/refresh",
                Map.of("refreshToken", "invalid.token.here"),
                Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("인증 없이 보호된 엔드포인트 → 401")
    void protectedEndpoint_noAuth() {
        ResponseEntity<Map> resp = restTemplate.getForEntity("/api/auth/me", Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
