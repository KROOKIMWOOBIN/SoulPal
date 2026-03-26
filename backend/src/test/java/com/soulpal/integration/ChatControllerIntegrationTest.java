package com.soulpal.integration;

import com.soulpal.dto.AuthResponse;
import com.soulpal.dto.RegisterRequest;
import com.soulpal.model.Character;
import com.soulpal.model.Message;
import com.soulpal.model.Project;
import com.soulpal.service.OllamaService;
import com.soulpal.service.WebCrawlerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * ChatController 통합 테스트.
 * 실제 PostgreSQL + Redis 컨테이너 위에서 채팅 API 전체 플로우를 검증합니다.
 * OllamaService / WebCrawlerService는 MockBean으로 대체합니다.
 */
@DisplayName("Chat API 통합 테스트")
class ChatControllerIntegrationTest extends IntegrationTestBase {

    @Autowired TestRestTemplate restTemplate;

    @MockBean OllamaService ollamaService;
    @MockBean WebCrawlerService webCrawlerService;

    private String accessToken;
    private String characterId;

    @BeforeEach
    void setUp() throws Exception {
        // OllamaService / WebCrawlerService 기본 동작 설정
        given(ollamaService.chat(anyString(), anyList(), anyString()))
                .willReturn("AI 테스트 응답");
        given(webCrawlerService.needsWebSearch(anyString())).willReturn(false);
        given(webCrawlerService.getWebContext(anyString())).willReturn("");

        // 매 테스트마다 독립적인 유저 생성
        String uniqueSuffix = String.valueOf(System.nanoTime());

        RegisterRequest req = new RegisterRequest();
        req.setUsername("chatUser" + uniqueSuffix);
        req.setEmail("chat" + uniqueSuffix + "@test.com");
        req.setPassword("Password123!");

        ResponseEntity<AuthResponse> regResp = restTemplate.postForEntity(
                "/api/auth/register", req, AuthResponse.class);
        accessToken = regResp.getBody().getAccessToken();

        // 프로젝트 생성
        HttpHeaders headers = bearerHeaders();
        ResponseEntity<Project> projectResp = restTemplate.exchange(
                "/api/projects", HttpMethod.POST,
                new HttpEntity<>(Map.of("name", "테스트 프로젝트"), headers),
                Project.class);
        String projectId = projectResp.getBody().getId();

        // 캐릭터 생성
        Map<String, Object> charReq = Map.of(
                "projectId", projectId,
                "name", "테스트캐릭터",
                "relationshipId", "bestfriend",
                "personalityIds", List.of("energetic"),
                "speechStyleIds", List.of("casual"),
                "interestIds", List.of("music"),
                "appearanceIds", List.of("cute")
        );
        ResponseEntity<Character> charResp = restTemplate.exchange(
                "/api/characters", HttpMethod.POST,
                new HttpEntity<>(charReq, headers), Character.class);
        characterId = charResp.getBody().getId();
    }

    // ── 메시지 목록 ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("메시지 목록 조회 — 초기 상태: 빈 리스트")
    void getMessages_empty() {
        ResponseEntity<List<Message>> resp = restTemplate.exchange(
                "/api/chat/messages/" + characterId,
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders()),
                new ParameterizedTypeReference<>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isEmpty();
    }

    @Test
    @DisplayName("메시지 목록 조회 — 페이징 파라미터 적용")
    void getMessages_pagination() {
        ResponseEntity<List<Message>> resp = restTemplate.exchange(
                "/api/chat/messages/" + characterId + "?page=0&size=10",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders()),
                new ParameterizedTypeReference<>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("메시지 목록 조회 — 인증 없이 접근 → 401")
    void getMessages_noAuth() {
        ResponseEntity<Map> resp = restTemplate.getForEntity(
                "/api/chat/messages/" + characterId, Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── 메시지 검색 ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("메시지 검색 — 매칭 없으면 빈 리스트")
    void searchMessages_noMatch() {
        ResponseEntity<List<Message>> resp = restTemplate.exchange(
                "/api/chat/messages/" + characterId + "/search?q=없는키워드",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders()),
                new ParameterizedTypeReference<>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isEmpty();
    }

    @Test
    @DisplayName("메시지 검색 — AI 메시지 저장 후 검색 성공")
    void searchMessages_afterSave() {
        // AI 메시지 저장
        Map<String, String> saveBody = Map.of(
                "characterId", characterId,
                "content", "검색할 수 있는 메시지 내용입니다"
        );
        restTemplate.exchange(
                "/api/chat/messages/save", HttpMethod.POST,
                new HttpEntity<>(saveBody, bearerHeaders()),
                Message.class);

        // 검색
        ResponseEntity<List<Message>> searchResp = restTemplate.exchange(
                "/api/chat/messages/" + characterId + "/search?q=검색할",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders()),
                new ParameterizedTypeReference<>() {});

        assertThat(searchResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(searchResp.getBody()).hasSizeGreaterThanOrEqualTo(1);
    }

    // ── AI 메시지 저장 ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("AI 메시지 저장 → 200 + 저장된 메시지 반환")
    void saveAiMessage_success() {
        Map<String, String> body = Map.of(
                "characterId", characterId,
                "content", "AI가 생성한 응답 메시지"
        );

        ResponseEntity<Message> resp = restTemplate.exchange(
                "/api/chat/messages/save", HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders()),
                Message.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getContent()).isEqualTo("AI가 생성한 응답 메시지");
        assertThat(resp.getBody().isUser()).isFalse();
    }

    // ── 마지막 AI 메시지 삭제 ────────────────────────────────────────────────────────

    @Test
    @DisplayName("마지막 AI 메시지 삭제 → 204")
    void deleteLastAiMessage_success() {
        // AI 메시지 먼저 저장
        restTemplate.exchange(
                "/api/chat/messages/save", HttpMethod.POST,
                new HttpEntity<>(Map.of("characterId", characterId, "content", "삭제 대상"), bearerHeaders()),
                Message.class);

        ResponseEntity<Void> resp = restTemplate.exchange(
                "/api/chat/messages/" + characterId + "/last-ai",
                HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders()),
                Void.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    // ── 대화 전체 삭제 ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("대화 전체 삭제 → 204 + 이후 목록 비어있음")
    void clearMessages_success() {
        // 메시지 저장
        restTemplate.exchange(
                "/api/chat/messages/save", HttpMethod.POST,
                new HttpEntity<>(Map.of("characterId", characterId, "content", "삭제될 메시지"), bearerHeaders()),
                Message.class);

        // 전체 삭제
        ResponseEntity<Void> deleteResp = restTemplate.exchange(
                "/api/chat/messages/" + characterId,
                HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders()),
                Void.class);
        assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // 목록 확인
        ResponseEntity<List<Message>> listResp = restTemplate.exchange(
                "/api/chat/messages/" + characterId,
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders()),
                new ParameterizedTypeReference<>() {});
        assertThat(listResp.getBody()).isEmpty();
    }

    // ── 권한 검사 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("다른 유저가 내 캐릭터 메시지 조회 → 403")
    void getMessages_otherUserCharacter_forbidden() {
        // 두 번째 유저 생성
        String uid = String.valueOf(System.nanoTime());
        RegisterRequest other = new RegisterRequest();
        other.setUsername("other" + uid);
        other.setEmail("other" + uid + "@test.com");
        other.setPassword("Password123!");
        ResponseEntity<AuthResponse> otherResp = restTemplate.postForEntity(
                "/api/auth/register", other, AuthResponse.class);
        String otherToken = otherResp.getBody().getAccessToken();

        HttpHeaders otherHeaders = new HttpHeaders();
        otherHeaders.setBearerAuth(otherToken);
        otherHeaders.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/chat/messages/" + characterId,
                HttpMethod.GET,
                new HttpEntity<>(otherHeaders),
                Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ── 동기 채팅 ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("동기 채팅(/send) — OllamaService 응답 → AI 메시지 반환")
    void sendMessage_success() {
        given(ollamaService.chat(anyString(), anyList(), anyString()))
                .willReturn("AI 동기 응답");

        Map<String, Object> body = Map.of(
                "characterId", characterId,
                "message", "안녕하세요!"
        );

        ResponseEntity<Message> resp = restTemplate.exchange(
                "/api/chat/send", HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders()),
                Message.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getContent()).isEqualTo("AI 동기 응답");
        assertThat(resp.getBody().isUser()).isFalse();
    }

    @Test
    @DisplayName("동기 채팅 — 2000자 초과 메시지 → 400")
    void sendMessage_messageTooLong() {
        Map<String, Object> body = Map.of(
                "characterId", characterId,
                "message", "A".repeat(2001)
        );

        ResponseEntity<Map> resp = restTemplate.exchange(
                "/api/chat/send", HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders()),
                Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── SSE 스트리밍 ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("SSE 스트리밍 엔드포인트 — 연결 성공 시 200 + text/event-stream")
    void streamChat_returnsSSEResponse() {
        // SSE는 응답 헤더만 검증 (스트림 내용은 비동기 처리)
        HttpHeaders headers = bearerHeaders();
        headers.setAccept(List.of(MediaType.TEXT_EVENT_STREAM));

        Map<String, Object> body = Map.of(
                "characterId", characterId,
                "message", "스트리밍 테스트"
        );

        ResponseEntity<String> resp = restTemplate.exchange(
                "/api/chat/stream", HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getHeaders().getContentType())
                .isNotNull()
                .satisfies(ct -> assertThat(ct.includes(MediaType.TEXT_EVENT_STREAM)).isTrue());
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────────────

    private HttpHeaders bearerHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
