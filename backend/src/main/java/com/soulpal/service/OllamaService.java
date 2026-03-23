package com.soulpal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.soulpal.model.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OllamaService {

    @Value("${ollama.base-url}")
    private String baseUrl;

    @Value("${ollama.model}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void streamChat(String systemPrompt, List<Message> history, String userMessage, SseEmitter emitter) {
        try {
            ArrayNode messages = objectMapper.createArrayNode();

            ObjectNode systemMsg = objectMapper.createObjectNode();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);
            messages.add(systemMsg);

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

            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.set("messages", messages);
            body.put("stream", true);

            byte[] bodyBytes = objectMapper.writeValueAsBytes(body);

            URL url = new URL(baseUrl + "/api/chat");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(120_000);
            conn.setDoOutput(true);
            conn.getOutputStream().write(bodyBytes);

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                StringBuilder fullResponse = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    JsonNode node = objectMapper.readTree(line);
                    boolean done = node.path("done").asBoolean(false);
                    String token = node.path("message").path("content").asText("");
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
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"error\": \"" + e.getMessage() + "\"}"));
            } catch (Exception ignored) {}
            emitter.completeWithError(e);
        }
    }

    public String chat(String systemPrompt, List<Message> history, String userMessage) throws Exception {
        ArrayNode messages = objectMapper.createArrayNode();

        ObjectNode systemMsg = objectMapper.createObjectNode();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        messages.add(systemMsg);

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

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.set("messages", messages);
        body.put("stream", false);

        byte[] bodyBytes = objectMapper.writeValueAsBytes(body);

        URL url = new URL(baseUrl + "/api/chat");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(120_000);
        conn.setDoOutput(true);
        conn.getOutputStream().write(bodyBytes);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            JsonNode node = objectMapper.readTree(sb.toString());
            return node.path("message").path("content").asText("");
        }
    }
}
