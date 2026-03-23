package com.soulpal.controller;

import com.soulpal.dto.ChatRequest;
import com.soulpal.model.Character;
import com.soulpal.model.Message;
import com.soulpal.service.CharacterService;
import com.soulpal.service.ContextBuilderService;
import com.soulpal.service.MessageService;
import com.soulpal.service.OllamaService;
import com.soulpal.service.WebCrawlerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final CharacterService characterService;
    private final MessageService messageService;
    private final OllamaService ollamaService;
    private final WebCrawlerService webCrawlerService;
    private final ContextBuilderService contextBuilderService;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    // 항상 포함할 최신 메시지 수 / 관련 과거 메시지 추가 수
    private static final int RECENT_COUNT = 10;
    private static final int RELEVANT_EXTRA = 5;

    @GetMapping("/messages/{characterId}")
    public List<Message> getMessages(
            @PathVariable String characterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        return messageService.getMessages(characterId, page, size);
    }

    @GetMapping("/messages/{characterId}/search")
    public List<Message> searchMessages(@PathVariable String characterId, @RequestParam String q) {
        return messageService.searchMessages(characterId, q);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@Valid @RequestBody ChatRequest req) {
        SseEmitter emitter = new SseEmitter(180_000L);

        executor.execute(() -> {
            try {
                Character character = characterService.getById(req.getCharacterId());
                String systemPrompt = characterService.buildSystemPrompt(character);

                // 개인화: 대화 이력 분석 컨텍스트 주입
                String userCtx = contextBuilderService.buildUserContext(req.getCharacterId());
                if (!userCtx.isBlank()) systemPrompt += userCtx;

                // RAG: 웹 검색 컨텍스트 주입
                boolean doSearch = req.isWebSearch() || webCrawlerService.needsWebSearch(req.getMessage());
                if (doSearch) {
                    String webCtx = webCrawlerService.getWebContext(req.getMessage());
                    if (!webCtx.isBlank()) systemPrompt += webCtx;
                }

                // 관련성 기반 히스토리 선택
                List<Message> history = contextBuilderService.getRelevantHistory(
                        req.getCharacterId(), req.getMessage(), RECENT_COUNT, RELEVANT_EXTRA);
                messageService.save(req.getCharacterId(), req.getMessage(), true);

                ollamaService.streamChat(systemPrompt, history, req.getMessage(), emitter);
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    @PostMapping("/send")
    public Message sendMessage(@Valid @RequestBody ChatRequest req) throws Exception {
        Character character = characterService.getById(req.getCharacterId());
        String systemPrompt = characterService.buildSystemPrompt(character);

        // 개인화: 대화 이력 분석 컨텍스트 주입
        String userCtx = contextBuilderService.buildUserContext(req.getCharacterId());
        if (!userCtx.isBlank()) systemPrompt += userCtx;

        boolean doSearch = req.isWebSearch() || webCrawlerService.needsWebSearch(req.getMessage());
        if (doSearch) {
            String webCtx = webCrawlerService.getWebContext(req.getMessage());
            if (!webCtx.isBlank()) systemPrompt += webCtx;
        }

        // 관련성 기반 히스토리 선택
        List<Message> history = contextBuilderService.getRelevantHistory(
                req.getCharacterId(), req.getMessage(), RECENT_COUNT, RELEVANT_EXTRA);
        messageService.save(req.getCharacterId(), req.getMessage(), true);

        String aiResponse = ollamaService.chat(systemPrompt, history, req.getMessage());
        Message aiMessage = messageService.save(req.getCharacterId(), aiResponse, false);
        characterService.updateLastMessage(req.getCharacterId(), aiResponse);

        return aiMessage;
    }

    @PostMapping("/messages/save")
    public Message saveAiMessage(@RequestBody Map<String, String> body) {
        String characterId = body.get("characterId");
        String content = body.get("content");
        Message msg = messageService.save(characterId, content, false);
        characterService.updateLastMessage(characterId, content);
        return msg;
    }

    @DeleteMapping("/messages/{characterId}/last-ai")
    public ResponseEntity<Void> deleteLastAiMessage(@PathVariable String characterId) {
        messageService.deleteLastAiMessage(characterId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/messages/{characterId}")
    public ResponseEntity<Void> clearMessages(@PathVariable String characterId) {
        messageService.clearAll(characterId);
        return ResponseEntity.noContent().build();
    }
}
