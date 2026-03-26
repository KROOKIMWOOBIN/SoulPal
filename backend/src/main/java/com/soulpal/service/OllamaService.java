package com.soulpal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.soulpal.exception.ErrorCode;
import com.soulpal.exception.BusinessException;
import com.soulpal.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Ollama REST API 클라이언트.
 * Java 11 HttpClient 로 커넥션 풀 재사용.
 */
@Slf4j
@Service
public class OllamaService {

    private static final long SLOW_THRESHOLD_MS  = 30_000;
    private static final int  MAX_RETRIES         = 2;
    private static final long RETRY_BASE_DELAY_MS = 1_000;

    @Value("${ollama.base-url}")
    private String baseUrl;

    @Value("${ollama.model}")
    private String model;

    // 커넥션 풀 재사용 (JVM 당 1개)
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void streamChat(String systemPrompt, List<Message> history,
                           String userMessage, SseEmitter emitter) {
        long start = System.currentTimeMillis();
        log.info("[OLLAMA] streamChat 시작: model={}, historySize={}, systemPromptLen={}",
                model, history.size(), systemPrompt.length());
        try {
            byte[] body = buildBody(systemPrompt, history, userMessage, true);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/chat"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(180))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            HttpResponse<java.io.InputStream> resp = httpClient.send(
                    req, HttpResponse.BodyHandlers.ofInputStream());

            log.debug("[OLLAMA] HTTP 응답: status={}", resp.statusCode());

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resp.body()))) {
                StringBuilder fullResponse = new StringBuilder();
                int tokenCount = 0;
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    JsonNode node  = objectMapper.readTree(line);
                    boolean done   = node.path("done").asBoolean(false);
                    String  token  = node.path("message").path("content").asText("");
                    if (!token.isEmpty()) {
                        fullResponse.append(token);
                        tokenCount++;
                    }
                    if (done) {
                        long duration = System.currentTimeMillis() - start;
                        if (duration > SLOW_THRESHOLD_MS) {
                            log.warn("[OLLAMA] 응답 지연: duration={}ms, tokens={}, model={}",
                                    duration, tokenCount, model);
                        } else {
                            log.info("[OLLAMA] streamChat 완료: duration={}ms, tokens={}, responseLen={}",
                                    duration, tokenCount, fullResponse.length());
                        }
                        emitter.send(SseEmitter.event()
                                .name("done")
                                .data(objectMapper.writeValueAsString(fullResponse.toString())));
                        emitter.complete();
                        return;
                    }
                }
            }
            emitter.complete();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.error("[OLLAMA] streamChat 오류: duration={}ms, model={}, error={}",
                    duration, model, e.getMessage(), e);
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"error\": \"AI 응답에 실패했습니다.\"}"));
            } catch (Exception ignored) {}
            emitter.completeWithError(e);
        }
    }

    /**
     * 스트리밍 응답을 콜백으로 처리합니다 (그룹 채팅용).
     * 각 토큰이 생성될 때마다 tokenConsumer가 호출됩니다.
     */
    public void streamChatWithCallback(String systemPrompt, List<Message> history,
                                       String userMessage, java.util.function.Consumer<String> tokenConsumer) {
        long start = System.currentTimeMillis();
        log.debug("[OLLAMA] streamChatWithCallback 시작: historySize={}", history.size());
        try {
            executeWithRetry(() -> {
                byte[] body = buildBody(systemPrompt, history, userMessage, true);
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/api/chat"))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(180))
                        .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                        .build();
                HttpResponse<java.io.InputStream> resp = httpClient.send(
                        req, HttpResponse.BodyHandlers.ofInputStream());
                int tokenCount = 0;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(resp.body()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.isBlank()) continue;
                        JsonNode node = objectMapper.readTree(line);
                        boolean done  = node.path("done").asBoolean(false);
                        String token  = node.path("message").path("content").asText("");
                        if (!token.isEmpty()) {
                            tokenConsumer.accept(token);
                            tokenCount++;
                        }
                        if (done) {
                            long duration = System.currentTimeMillis() - start;
                            if (duration > SLOW_THRESHOLD_MS) {
                                log.warn("[OLLAMA] 응답 지연 (callback): duration={}ms, tokens={}", duration, tokenCount);
                            } else {
                                log.debug("[OLLAMA] streamChatWithCallback 완료: duration={}ms, tokens={}", duration, tokenCount);
                            }
                            return null;
                        }
                    }
                }
                return null;
            });
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.error("[OLLAMA] streamChatWithCallback 오류: duration={}ms, error={}",
                    duration, e.getMessage(), e);
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
    }

    public String chat(String systemPrompt, List<Message> history, String userMessage) {
        long start = System.currentTimeMillis();
        log.info("[OLLAMA] chat 시작: model={}, historySize={}", model, history.size());
        try {
            return executeWithRetry(() -> {
                byte[] body = buildBody(systemPrompt, history, userMessage, false);
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/api/chat"))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(120))
                        .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                        .build();
                HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                JsonNode node = objectMapper.readTree(resp.body());
                String response = node.path("message").path("content").asText("");
                long duration = System.currentTimeMillis() - start;
                if (duration > SLOW_THRESHOLD_MS) {
                    log.warn("[OLLAMA] 응답 지연: duration={}ms, responseLen={}", duration, response.length());
                } else {
                    log.info("[OLLAMA] chat 완료: duration={}ms, responseLen={}", duration, response.length());
                }
                return response;
            });
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.error("[OLLAMA] chat 오류: duration={}ms, model={}, error={}",
                    duration, model, e.getMessage(), e);
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
    }

    // ── 내부 헬퍼 ─────────────────────────────────────────────────────────────

    /** 최대 MAX_RETRIES 횟수만큼 지수 백오프 재시도. */
    private <T> T executeWithRetry(Callable<T> action) throws Exception {
        Exception lastException = null;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                return action.call();
            } catch (Exception e) {
                lastException = e;
                if (attempt < MAX_RETRIES) {
                    long delay = RETRY_BASE_DELAY_MS * (1L << attempt);
                    log.warn("[OLLAMA] 재시도 {}/{}: delay={}ms, error={}", attempt + 1, MAX_RETRIES, delay, e.getMessage());
                    try { Thread.sleep(delay); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); throw ie; }
                }
            }
        }
        throw lastException;
    }

    private byte[] buildBody(String systemPrompt, List<Message> history,
                             String userMessage, boolean stream) throws Exception {
        ArrayNode messages = objectMapper.createArrayNode();

        ObjectNode sys = objectMapper.createObjectNode();
        sys.put("role", "system");
        sys.put("content", systemPrompt);
        messages.add(sys);

        for (Message m : history) {
            ObjectNode msg = objectMapper.createObjectNode();
            msg.put("role", m.isUser() ? "user" : "assistant");
            msg.put("content", m.getContent());
            messages.add(msg);
        }

        ObjectNode userMsg = objectMapper.createObjectNode();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        ObjectNode reqBody = objectMapper.createObjectNode();
        reqBody.put("model", model);
        reqBody.set("messages", messages);
        reqBody.put("stream", stream);

        return objectMapper.writeValueAsBytes(reqBody);
    }
}
