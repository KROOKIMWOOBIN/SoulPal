package com.soulpal.service;

import com.soulpal.model.Message;
import com.soulpal.repository.MessageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContextBuilderService 테스트")
class ContextBuilderServiceTest {

    @Mock MessageRepository messageRepository;

    @InjectMocks ContextBuilderService contextBuilderService;

    // ── buildUserContext ───────────────────────────────────────────────────────

    @Test
    @DisplayName("메시지 4개 미만 → 빈 문자열 반환")
    void buildUserContext_tooFewMessages() {
        given(messageRepository.countByCharacterId("char-1")).willReturn(3L);

        String result = contextBuilderService.buildUserContext("char-1");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("사용자 메시지 없음 → 빈 문자열 반환")
    void buildUserContext_noUserMessages() {
        given(messageRepository.countByCharacterId("char-1")).willReturn(10L);
        // AI 메시지만 존재 (isUser = false)
        List<Message> aiOnly = List.of(
                buildMessage("안녕하세요!", false),
                buildMessage("무엇을 도와드릴까요?", false)
        );
        given(messageRepository.findByCharacterIdOrderByCreatedAtDesc(eq("char-1"), any(PageRequest.class)))
                .willReturn(aiOnly);

        String result = contextBuilderService.buildUserContext("char-1");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("충분한 대화 → 개인화 컨텍스트 문자열 반환")
    void buildUserContext_withSufficientMessages() {
        given(messageRepository.countByCharacterId("char-1")).willReturn(20L);

        List<Message> messages = new ArrayList<>();
        // 반복되는 키워드 포함
        for (int i = 0; i < 10; i++) {
            messages.add(buildMessage("음악 좋아해 음악이 너무 좋아", true));
        }
        given(messageRepository.findByCharacterIdOrderByCreatedAtDesc(eq("char-1"), any(PageRequest.class)))
                .willReturn(messages);

        String result = contextBuilderService.buildUserContext("char-1");

        assertThat(result).isNotEmpty();
        assertThat(result).contains("사용자 분석");
    }

    @Test
    @DisplayName("긍정 메시지 다수 → 밝고 긍정적 감정 컨텍스트 포함")
    void buildUserContext_positiveEmotionalTone() {
        given(messageRepository.countByCharacterId("char-1")).willReturn(20L);

        List<Message> messages = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            messages.add(buildMessage("오늘 너무 행복해 정말 좋아", true));
        }
        given(messageRepository.findByCharacterIdOrderByCreatedAtDesc(eq("char-1"), any(PageRequest.class)))
                .willReturn(messages);

        String result = contextBuilderService.buildUserContext("char-1");

        assertThat(result).contains("긍정");
    }

    @Test
    @DisplayName("부정 메시지 다수 → 따뜻한 공감 감정 컨텍스트 포함")
    void buildUserContext_negativeEmotionalTone() {
        given(messageRepository.countByCharacterId("char-1")).willReturn(20L);

        List<Message> messages = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            messages.add(buildMessage("요즘 너무 힘들어 우울해 피곤해", true));
        }
        given(messageRepository.findByCharacterIdOrderByCreatedAtDesc(eq("char-1"), any(PageRequest.class)))
                .willReturn(messages);

        String result = contextBuilderService.buildUserContext("char-1");

        assertThat(result).contains("공감");
    }

    // ── getRelevantHistory ─────────────────────────────────────────────────────

    @Test
    @DisplayName("최근 N개만 반환 (이전 메시지 없음)")
    void getRelevantHistory_onlyRecent() {
        List<Message> recent = new ArrayList<>(List.of(
                buildMessage("최근 메시지", true),
                buildMessage("AI 응답", false)
        ));
        given(messageRepository.findByCharacterIdOrderByCreatedAtDesc(eq("char-1"), any(PageRequest.class)))
                .willReturn(recent);
        given(messageRepository.countByCharacterId("char-1")).willReturn(2L);

        List<Message> result = contextBuilderService.getRelevantHistory("char-1", "테스트", 10, 3);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("현재 메시지와 관련 있는 과거 메시지 포함")
    void getRelevantHistory_withRelevant() {
        List<Message> recent = List.of(buildMessage("최근 메시지", true));
        Message relevant = buildMessage("음악 얘기했던 과거 메시지", true);

        given(messageRepository.countByCharacterId("char-1")).willReturn(50L);
        given(messageRepository.findByCharacterIdOrderByCreatedAtDesc(eq("char-1"), any(PageRequest.class)))
                .willReturn(recent)
                .willReturn(List.of(buildMessage("최근 메시지", true), relevant));

        List<Message> result = contextBuilderService.getRelevantHistory("char-1", "음악 좋아해", 1, 3);

        assertThat(result).isNotEmpty();
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────────

    private Message buildMessage(String content, boolean isUser) {
        return Message.builder()
                .id(java.util.UUID.randomUUID().toString())
                .content(content)
                .user(isUser)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
