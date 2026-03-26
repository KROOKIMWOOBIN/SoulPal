package com.soulpal.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * WebCrawlerService 단위 테스트
 *
 * 외부 HTTP 호출(DuckDuckGo, 실제 URL 크롤링)은 네트워크 의존성이 있어
 * 공개 메서드 중 순수 로직인 needsWebSearch()와
 * private 메서드 간접 검증(getWebContext 실패 시 빈 문자열)에 집중합니다.
 */
@DisplayName("WebCrawlerService 테스트")
class WebCrawlerServiceTest {

    private final WebCrawlerService webCrawlerService = new WebCrawlerService();

    // ── needsWebSearch ────────────────────────────────────────────────────────

    @Test
    @DisplayName("검색 트리거 키워드 포함 → true")
    void needsWebSearch_triggerKeyword_returnsTrue() {
        assertThat(webCrawlerService.needsWebSearch("오늘 날씨 알려줘")).isTrue();
    }

    @Test
    @DisplayName("'뭐야' 포함 → true")
    void needsWebSearch_mwoya_returnsTrue() {
        assertThat(webCrawlerService.needsWebSearch("그게 뭐야?")).isTrue();
    }

    @Test
    @DisplayName("'최신' 포함 → true")
    void needsWebSearch_latest_returnsTrue() {
        assertThat(webCrawlerService.needsWebSearch("최신 뉴스 보여줘")).isTrue();
    }

    @Test
    @DisplayName("'?' 포함 → true")
    void needsWebSearch_questionMark_returnsTrue() {
        assertThat(webCrawlerService.needsWebSearch("넌 누구야?")).isTrue();
    }

    @Test
    @DisplayName("영어 what 포함 → true (case insensitive)")
    void needsWebSearch_englishWhat_returnsTrue() {
        assertThat(webCrawlerService.needsWebSearch("What is the weather today")).isTrue();
    }

    @Test
    @DisplayName("영어 HOW 대문자 → true (case insensitive)")
    void needsWebSearch_upperCaseHow_returnsTrue() {
        assertThat(webCrawlerService.needsWebSearch("HOW does it work")).isTrue();
    }

    @Test
    @DisplayName("트리거 없는 일반 메시지 → false")
    void needsWebSearch_noTrigger_returnsFalse() {
        assertThat(webCrawlerService.needsWebSearch("오늘도 잘 지냈어")).isFalse();
    }

    @Test
    @DisplayName("빈 메시지 → false")
    void needsWebSearch_empty_returnsFalse() {
        assertThat(webCrawlerService.needsWebSearch("")).isFalse();
    }

    @Test
    @DisplayName("'찾아줘' 포함 → true")
    void needsWebSearch_findKeyword_returnsTrue() {
        assertThat(webCrawlerService.needsWebSearch("맛집 찾아줘")).isTrue();
    }

    @Test
    @DisplayName("'정보' 포함 → true")
    void needsWebSearch_infoKeyword_returnsTrue() {
        assertThat(webCrawlerService.needsWebSearch("스프링부트 정보 알려줘")).isTrue();
    }

    // ── getWebContext (네트워크 실패 시 graceful 처리) ───────────────────────────

    @Test
    @DisplayName("접근 불가 URL 검색 → 예외 없이 빈 문자열 반환")
    void getWebContext_unreachableNetwork_returnsEmpty() {
        // 실제 DuckDuckGo 연결이 실패해도 graceful하게 "" 반환되어야 함
        // CI 환경에서 네트워크 없을 때도 동작 보장
        String result = webCrawlerService.getWebContext("test query 12345 xyz");
        // 결과는 빈 문자열이거나 실제 컨텍스트 (네트워크 유무에 따라)
        // 핵심: 예외를 던지지 않아야 함
        assertThat(result).isNotNull();
    }
}
