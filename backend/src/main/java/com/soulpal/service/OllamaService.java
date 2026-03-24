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

/**
 * Ollama REST API 클라이언트.
 * Java 11 HttpClient 로 커넥션 풀 재사용.
 */
@Slf4j
@Service
public class OllamaService {

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

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resp.body()))) {
                StringBuilder fullResponse = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    JsonNode node  = objectMapper.readTree(line);
                    boolean done   = node.path("done").asBoolean(false);
                    String  token  = node.path("message").path("content").asText("");
                    if (!token.isEmpty()) {
                        fullResponse.append(token);
                        emitter.send(SseEmitter.event()
                                .name("token")
                                .data(objectMapper.writeValueAsString(token)));
                    }
                    if (done) {
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
            log.error("[Ollama] streamChat 오류: {}", e.getMessage(), e);
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"error\": \"AI 응답에 실패했습니다.\"}"));
            } catch (Exception ignored) {}
            emitter.completeWithError(e);
        }
    }

    public String chat(String systemPrompt, List<Message> history, String userMessage) {
        try {
            byte[] body = buildBody(systemPrompt, history, userMessage, false);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/chat"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(120))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode node = objectMapper.readTree(resp.body());
            return node.path("message").path("content").asText("");

        } catch (Exception e) {
            log.error("[Ollama] chat 오류: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
    }

    // ── 내부 헬퍼 ─────────────────────────────────────────────────────────────

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
