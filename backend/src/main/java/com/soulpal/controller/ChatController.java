package com.soulpal.controller;

import com.soulpal.dto.ChatRequest;
import com.soulpal.model.Character;
import com.soulpal.model.Message;
import com.soulpal.service.CharacterService;
import com.soulpal.service.ContextBuilderService;
import com.soulpal.service.MessageService;
import com.soulpal.service.OllamaService;
import com.soulpal.service.RateLimitService;
import com.soulpal.service.WebCrawlerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

@Slf4j
@Tag(name = "Chat", description = "채팅 API")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final CharacterService characterService;
    private final MessageService messageService;
    private final OllamaService ollamaService;
    private final WebCrawlerService webCrawlerService;
    private final ContextBuilderService contextBuilderService;
    private final RateLimitService rateLimitService;
    @Qualifier("sseExecutor")
    private final ExecutorService executor;

    private static final int RECENT_COUNT   = 10;
    private static final int RELEVANT_EXTRA = 5;

    @Operation(summary = "메시지 목록 조회 (페이징)")
    @GetMapping("/messages/{characterId}")
    public List<Message> getMessages(
            @PathVariable String characterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            Authentication auth) {
        characterService.verifyOwnership(characterId, (String) auth.getPrincipal());
        return messageService.getMessages(characterId, page, size);
    }

    @Operation(summary = "메시지 검색")
    @GetMapping("/messages/{characterId}/search")
    public List<Message> searchMessages(
            @PathVariable String characterId,
            @RequestParam String q,
            Authentication auth) {
        characterService.verifyOwnership(characterId, (String) auth.getPrincipal());
        log.debug("[CHAT] 메시지 검색: characterId={}, keyword={}", characterId, q);
        return messageService.searchMessages(characterId, q);
    }

    @Operation(summary = "SSE 스트리밍 채팅")
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@Valid @RequestBody ChatRequest req, Authentication auth) {
        String userId = (String) auth.getPrincipal();
        rateLimitService.checkChat(userId);

        SseEmitter emitter = new SseEmitter(180_000L);

        // MDC + SecurityContext를 executor 스레드로 전파
        Map<String, String> mdcCtx = MDC.getCopyOfContextMap();
        SecurityContext securityContext = SecurityContextHolder.getContext();

        executor.execute(() -> {
            if (mdcCtx != null) MDC.setContextMap(mdcCtx);
            SecurityContextHolder.setContext(securityContext);
            long start = System.currentTimeMillis();
            try {
                Character character = characterService.getById(req.getCharacterId(), userId);
                log.info("[CHAT] 스트림 시작: characterId={}, characterName={}, msgLen={}",
                        req.getCharacterId(), character.getName(), req.getMessage().length());

                String systemPrompt = characterService.buildSystemPrompt(character);

                // 개인화 컨텍스트
                String userCtx = contextBuilderService.buildUserContext(req.getCharacterId());
                if (!userCtx.isBlank()) {
                    systemPrompt += userCtx;
                    log.debug("[CHAT] 개인화 컨텍스트 적용: characterId={}", req.getCharacterId());
                }

                // 웹 검색 여부 결정
                boolean doSearch = req.isWebSearch() || webCrawlerService.needsWebSearch(req.getMessage());
                if (doSearch) {
                    log.info("[CHAT] 웹 검색 실행: query={}", req.getMessage());
                    String webCtx = webCrawlerService.getWebContext(req.getMessage());
                    if (!webCtx.isBlank()) {
                        systemPrompt += webCtx;
                        log.debug("[CHAT] 웹 컨텍스트 추가: len={}", webCtx.length());
                    }
                }

                // 히스토리 조회
                List<Message> history = contextBuilderService.getRelevantHistory(
                        req.getCharacterId(), req.getMessage(), RECENT_COUNT, RELEVANT_EXTRA);
                log.debug("[CHAT] 히스토리 구성: count={}, systemPromptLen={}",
                        history.size(), systemPrompt.length());

                messageService.save(req.getCharacterId(), req.getMessage(), true);

                ollamaService.streamChat(systemPrompt, history, req.getMessage(), emitter);

                log.info("[CHAT] 스트림 완료: characterId={}, duration={}ms",
                        req.getCharacterId(), System.currentTimeMillis() - start);

            } catch (Exception e) {
                log.error("[CHAT] 스트림 오류: characterId={}, duration={}ms, error={}",
                        req.getCharacterId(), System.currentTimeMillis() - start, e.getMessage(), e);
                emitter.completeWithError(e);
            } finally {
                MDC.clear();
                SecurityContextHolder.clearContext();
            }
        });

        return emitter;
    }

    @Operation(summary = "일반 채팅 (동기)")
    @PostMapping("/send")
    public Message sendMessage(@Valid @RequestBody ChatRequest req, Authentication auth) throws Exception {
        String userId = (String) auth.getPrincipal();
        rateLimitService.checkChat(userId);

        long start = System.currentTimeMillis();
        Character character = characterService.getById(req.getCharacterId(), userId);
        log.info("[CHAT] 동기 전송: characterId={}, characterName={}, msgLen={}",
                req.getCharacterId(), character.getName(), req.getMessage().length());

        String systemPrompt = characterService.buildSystemPrompt(character);

        String userCtx = contextBuilderService.buildUserContext(req.getCharacterId());
        if (!userCtx.isBlank()) systemPrompt += userCtx;

        boolean doSearch = req.isWebSearch() || webCrawlerService.needsWebSearch(req.getMessage());
        if (doSearch) {
            log.info("[CHAT] 웹 검색 실행: query={}", req.getMessage());
            String webCtx = webCrawlerService.getWebContext(req.getMessage());
            if (!webCtx.isBlank()) systemPrompt += webCtx;
        }

        List<Message> history = contextBuilderService.getRelevantHistory(
                req.getCharacterId(), req.getMessage(), RECENT_COUNT, RELEVANT_EXTRA);
        messageService.save(req.getCharacterId(), req.getMessage(), true);

        String aiResponse = ollamaService.chat(systemPrompt, history, req.getMessage());
        Message aiMessage = messageService.save(req.getCharacterId(), aiResponse, false);
        characterService.updateLastMessage(req.getCharacterId(), userId, aiResponse);

        log.info("[CHAT] 동기 완료: characterId={}, responseLen={}, duration={}ms",
                req.getCharacterId(), aiResponse.length(), System.currentTimeMillis() - start);
        return aiMessage;
    }

    @Operation(summary = "AI 메시지 저장")
    @PostMapping("/messages/save")
    public Message saveAiMessage(@RequestBody Map<String, String> body, Authentication auth) {
        String characterId = body.get("characterId");
        String content     = body.get("content");
        characterService.verifyOwnership(characterId, (String) auth.getPrincipal());
        Message msg = messageService.save(characterId, content, false);
        characterService.updateLastMessage(characterId, content);
        log.debug("[CHAT] AI 메시지 저장: characterId={}, len={}", characterId, content.length());
        return msg;
    }

    @Operation(summary = "마지막 AI 메시지 삭제")
    @DeleteMapping("/messages/{characterId}/last-ai")
    public ResponseEntity<Void> deleteLastAiMessage(
            @PathVariable String characterId, Authentication auth) {
        characterService.verifyOwnership(characterId, (String) auth.getPrincipal());
        messageService.deleteLastAiMessage(characterId);
        log.info("[CHAT] 마지막 AI 메시지 삭제: characterId={}", characterId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "대화 전체 삭제")
    @DeleteMapping("/messages/{characterId}")
    public ResponseEntity<Void> clearMessages(
            @PathVariable String characterId, Authentication auth) {
        characterService.verifyOwnership(characterId, (String) auth.getPrincipal());
        messageService.clearAll(characterId);
        log.info("[CHAT] 대화 전체 삭제: characterId={}", characterId);
        return ResponseEntity.noContent().build();
    }
}
