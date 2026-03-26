package com.soulpal.service;

import com.soulpal.model.Message;
import com.soulpal.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageService 테스트")
class MessageServiceTest {

    @Mock MessageRepository messageRepository;

    @InjectMocks MessageService messageService;

    private Message userMsg;
    private Message aiMsg;

    @BeforeEach
    void setUp() {
        userMsg = Message.builder()
                .id("msg-1")
                .characterId("char-1")
                .content("안녕!")
                .user(true)
                .createdAt(LocalDateTime.now().minusMinutes(2))
                .build();

        aiMsg = Message.builder()
                .id("msg-2")
                .characterId("char-1")
                .content("안녕하세요!")
                .user(false)
                .createdAt(LocalDateTime.now().minusMinutes(1))
                .build();
    }

    // ── getMessages ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("메시지 조회 → desc → 역순으로 반환 (asc)")
    void getMessages_returnsAscending() {
        // Repository는 desc로 반환, service에서 reverse
        given(messageRepository.findByCharacterIdOrderByCreatedAtDesc(eq("char-1"), any()))
                .willReturn(new ArrayList<>(List.of(aiMsg, userMsg)));

        List<Message> result = messageService.getMessages("char-1", 0, 30);

        // reverse 후 asc 순서 (userMsg가 먼저)
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo("msg-1"); // userMsg (더 오래됨)
        assertThat(result.get(1).getId()).isEqualTo("msg-2"); // aiMsg (더 최근)
    }

    @Test
    @DisplayName("메시지 없으면 빈 목록 반환")
    void getMessages_empty() {
        given(messageRepository.findByCharacterIdOrderByCreatedAtDesc(eq("char-1"), any()))
                .willReturn(List.of());

        List<Message> result = messageService.getMessages("char-1", 0, 30);

        assertThat(result).isEmpty();
    }

    // ── searchMessages ────────────────────────────────────────────────────────

    @Test
    @DisplayName("키워드 검색 → 일치하는 메시지 반환")
    void searchMessages_returnsMatches() {
        given(messageRepository.searchByContent("char-1", "안녕"))
                .willReturn(List.of(userMsg));

        List<Message> result = messageService.searchMessages("char-1", "안녕");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("안녕!");
    }

    @Test
    @DisplayName("키워드 검색 결과 없으면 빈 목록")
    void searchMessages_noMatch() {
        given(messageRepository.searchByContent("char-1", "없는말"))
                .willReturn(List.of());

        List<Message> result = messageService.searchMessages("char-1", "없는말");

        assertThat(result).isEmpty();
    }

    // ── save ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("유저 메시지 저장 → user=true 로 저장")
    void save_userMessage() {
        given(messageRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        Message saved = messageService.save("char-1", "테스트", true);

        assertThat(saved.isUser()).isTrue();
        assertThat(saved.getContent()).isEqualTo("테스트");
        assertThat(saved.getCharacterId()).isEqualTo("char-1");
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("AI 메시지 저장 → user=false 로 저장")
    void save_aiMessage() {
        given(messageRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        Message saved = messageService.save("char-1", "AI 응답", false);

        assertThat(saved.isUser()).isFalse();
    }

    // ── deleteLastAiMessage ───────────────────────────────────────────────────

    @Test
    @DisplayName("마지막 메시지가 AI 메시지면 삭제")
    void deleteLastAiMessage_deletesIfAi() {
        given(messageRepository.findByCharacterIdOrderByCreatedAtDesc(eq("char-1"), any()))
                .willReturn(List.of(aiMsg));

        messageService.deleteLastAiMessage("char-1");

        then(messageRepository).should().delete(aiMsg);
    }

    @Test
    @DisplayName("마지막 메시지가 유저 메시지면 삭제 안 함")
    void deleteLastAiMessage_skipsIfUser() {
        given(messageRepository.findByCharacterIdOrderByCreatedAtDesc(eq("char-1"), any()))
                .willReturn(List.of(userMsg));

        messageService.deleteLastAiMessage("char-1");

        then(messageRepository).should(never()).delete(any());
    }

    @Test
    @DisplayName("메시지 없으면 아무것도 안 함")
    void deleteLastAiMessage_emptyList() {
        given(messageRepository.findByCharacterIdOrderByCreatedAtDesc(eq("char-1"), any()))
                .willReturn(List.of());

        messageService.deleteLastAiMessage("char-1");

        then(messageRepository).should(never()).delete(any());
    }

    // ── clearAll ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("모든 메시지 삭제 → deleteByCharacterId 호출")
    void clearAll_deletesAll() {
        messageService.clearAll("char-1");

        then(messageRepository).should().deleteByCharacterId("char-1");
    }

    // ── countMessages ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("메시지 카운트 → repository count 반환")
    void countMessages_returnsCount() {
        given(messageRepository.countByCharacterId("char-1")).willReturn(42L);

        long count = messageService.countMessages("char-1");

        assertThat(count).isEqualTo(42L);
    }

    // ── getRecentForContext ───────────────────────────────────────────────────

    @Test
    @DisplayName("컨텍스트용 최근 N개 조회 → asc 순서 반환")
    void getRecentForContext_returnsAscending() {
        given(messageRepository.findByCharacterIdOrderByCreatedAtDesc(eq("char-1"), eq(PageRequest.of(0, 5))))
                .willReturn(new ArrayList<>(List.of(aiMsg, userMsg)));

        List<Message> result = messageService.getRecentForContext("char-1", 5);

        assertThat(result.get(0).getId()).isEqualTo("msg-1"); // userMsg 먼저 (오래됨)
    }
}
