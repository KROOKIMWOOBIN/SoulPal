package com.soulpal.service;

import com.soulpal.exception.BusinessException;
import com.soulpal.model.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("OllamaService 테스트")
class OllamaServiceTest {

    private OllamaService ollamaService;

    @BeforeEach
    void setUp() throws Exception {
        ollamaService = new OllamaService();

        // baseUrl / model 필드 주입 (reflection)
        var baseUrlField = OllamaService.class.getDeclaredField("baseUrl");
        baseUrlField.setAccessible(true);
        baseUrlField.set(ollamaService, "http://localhost:11434");

        var modelField = OllamaService.class.getDeclaredField("model");
        modelField.setAccessible(true);
        modelField.set(ollamaService, "llama3");
    }

    // ── streamChat ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("streamChat — Ollama 연결 불가 시 emitter에 error 이벤트 전송")
    void streamChat_connectionFails_sendsErrorEvent() throws Exception {
        // 실제 HTTP 요청 발생 → localhost:11434 는 테스트 환경에서 연결 불가
        // → catch 블록으로 진입하여 emitter.completeWithError() 호출 확인
        SseEmitter emitter = mock(SseEmitter.class);
        Message msg = mock(Message.class);
        given(msg.isUser()).willReturn(true);
        given(msg.getContent()).willReturn("hi");

        ollamaService.streamChat("system", List.of(msg), "hello", emitter);

        verify(emitter, atLeastOnce()).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter).completeWithError(any(Exception.class));
    }

    // ── streamChatWithCallback ────────────────────────────────────────────────

    @Test
    @DisplayName("streamChatWithCallback — 연결 불가 시 BusinessException 발생")
    void streamChatWithCallback_connectionFails_throwsBusinessException() {
        assertThatThrownBy(() ->
                ollamaService.streamChatWithCallback("system", List.of(), "hello", token -> {}))
                .isInstanceOf(BusinessException.class);
    }

    // ── chat ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("chat — 연결 불가 시 BusinessException 발생")
    void chat_connectionFails_throwsBusinessException() {
        assertThatThrownBy(() ->
                ollamaService.chat("system", List.of(), "hello"))
                .isInstanceOf(BusinessException.class);
    }

    // ── given helper (BDDMockito 없이 plain Mockito) ─────────────────────────

    private static <T> org.mockito.stubbing.OngoingStubbing<T> given(T methodCall) {
        return org.mockito.Mockito.when(methodCall);
    }
}
