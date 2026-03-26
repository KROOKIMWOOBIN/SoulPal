package com.soulpal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soulpal.exception.ResourceNotFoundException;
import com.soulpal.model.Character;
import com.soulpal.model.GroupMessage;
import com.soulpal.model.GroupRoom;
import com.soulpal.model.Message;
import com.soulpal.repository.GroupMessageRepository;
import com.soulpal.repository.GroupRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupChatService {

    private final GroupRoomRepository groupRoomRepository;
    private final GroupMessageRepository groupMessageRepository;
    private final CharacterService characterService;
    private final OllamaService ollamaService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int HISTORY_LIMIT = 20; // 그룹 채팅 히스토리 조회 수

    // ── 방 관리 ─────────────────────────────────────────────────────────────────

    @Transactional
    public GroupRoom createRoom(String userId, String projectId, String name, List<String> characterIds) {
        // 모든 캐릭터가 해당 유저 소유인지 검증
        for (String charId : characterIds) {
            characterService.verifyOwnership(charId, userId);
        }
        GroupRoom room = GroupRoom.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .projectId(projectId)
                .name(name)
                .characterIds(characterIds)
                .createdAt(LocalDateTime.now())
                .build();
        groupRoomRepository.save(room);
        log.info("[GROUP] 방 생성: id={}, name={}, userId={}, characters={}",
                room.getId(), name, userId, characterIds.size());
        return room;
    }

    public List<GroupRoom> getRooms(String userId, String projectId) {
        if (projectId != null && !projectId.isBlank()) {
            return groupRoomRepository
                    .findAllByUserIdAndProjectIdOrderByLastMessageAtDescCreatedAtDesc(userId, projectId);
        }
        return groupRoomRepository.findAllByUserIdOrderByLastMessageAtDescCreatedAtDesc(userId);
    }

    public GroupRoom getRoom(String roomId, String userId) {
        return groupRoomRepository.findByIdAndUserId(roomId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("그룹 대화방을 찾을 수 없습니다: " + roomId));
    }

    @Transactional
    public void deleteRoom(String roomId, String userId) {
        GroupRoom room = getRoom(roomId, userId);
        groupMessageRepository.deleteByRoomId(roomId);
        groupRoomRepository.delete(room);
    }

    public List<GroupMessage> getMessages(String roomId, String userId, int page, int size) {
        getRoom(roomId, userId); // 소유권 검증
        List<GroupMessage> desc = groupMessageRepository.findByRoomIdOrderByCreatedAtDesc(
                roomId, PageRequest.of(page, size));
        List<GroupMessage> asc = new ArrayList<>(desc);
        java.util.Collections.reverse(asc);
        return asc;
    }

    // ── 그룹 스트리밍 채팅 ───────────────────────────────────────────────────────

    /**
     * 사용자 메시지를 저장한 뒤, 방에 초대된 모든 캐릭터가 순서대로 응답합니다.
     *
     * SSE 이벤트 형식:
     *   event: char-start  data: {"characterId": "...", "characterName": "..."}
     *   event: token       data: "<토큰>"
     *   event: char-done   data: {"characterId": "...", "message": "전체 응답"}
     *   event: done        data: {}
     */
    public void streamGroupChat(String roomId, String userId, String userMessage, SseEmitter emitter) {
        long streamStart = System.currentTimeMillis();
        try {
            GroupRoom room = getRoom(roomId, userId);
            log.info("[GROUP] 스트림 시작: roomId={}, characters={}, msgLen={}",
                    roomId, room.getCharacterIds().size(), userMessage.length());

            // 1. 사용자 메시지 저장
            GroupMessage userMsg = saveMessage(roomId, userMessage, null, "나");

            // 2. 최근 히스토리 조회 (모든 캐릭터 공유)
            List<GroupMessage> history = groupMessageRepository.findByRoomIdOrderByCreatedAtAsc(roomId);
            // 최신 HISTORY_LIMIT개로 제한 (방금 저장한 메시지 제외)
            List<GroupMessage> contextHistory = history.stream()
                    .filter(m -> !m.getId().equals(userMsg.getId()))
                    .collect(Collectors.toList());
            if (contextHistory.size() > HISTORY_LIMIT) {
                contextHistory = contextHistory.subList(contextHistory.size() - HISTORY_LIMIT, contextHistory.size());
            }

            // 3. 각 캐릭터 순서대로 응답 (배치 로딩으로 N+1 방지)
            Map<String, Character> characterMap = characterService.getByIds(room.getCharacterIds());
            String lastUserMessage = userMessage;
            List<GroupMessage> currentContext = new ArrayList<>(contextHistory);

            for (String characterId : room.getCharacterIds()) {
                Character character = characterMap.get(characterId);
                if (character == null) {
                    log.warn("[GroupChat] 캐릭터 조회 실패: {}", characterId);
                    continue;
                }

                // char-start 이벤트
                emitter.send(SseEmitter.event()
                        .name("char-start")
                        .data(objectMapper.writeValueAsString(Map.of(
                                "characterId", character.getId(),
                                "characterName", character.getName()
                        ))));

                // 시스템 프롬프트 + 그룹 맥락 추가
                String systemPrompt = characterService.buildSystemPrompt(character)
                        + buildGroupContext(room, character, currentContext, characterMap);

                // Message 변환 (OllamaService 호환)
                List<Message> ollamaHistory = toOllamaHistory(currentContext, character.getId());

                // 스트리밍 응답
                long charStart = System.currentTimeMillis();
                StringBuilder fullResponse = new StringBuilder();
                ollamaService.streamChatWithCallback(
                        systemPrompt, ollamaHistory, lastUserMessage,
                        token -> fullResponse.append(token)
                );

                String aiResponse = fullResponse.toString().trim();
                if (aiResponse.isBlank()) aiResponse = "...";
                log.info("[GROUP] 캐릭터 응답 완료: characterId={}, name={}, duration={}ms, responseLen={}",
                        character.getId(), character.getName(),
                        System.currentTimeMillis() - charStart, aiResponse.length());

                // AI 응답 저장
                GroupMessage aiMsg = saveMessage(roomId, aiResponse, character.getId(), character.getName());
                currentContext.add(aiMsg);

                // char-done 이벤트
                emitter.send(SseEmitter.event()
                        .name("char-done")
                        .data(objectMapper.writeValueAsString(Map.of(
                                "characterId", character.getId(),
                                "messageId", aiMsg.getId(),
                                "message", aiResponse
                        ))));
            }

            // 방의 마지막 메시지 업데이트
            updateRoomLastMessage(room, userMessage);

            log.info("[GROUP] 스트림 완료: roomId={}, totalDuration={}ms",
                    roomId, System.currentTimeMillis() - streamStart);
            emitter.send(SseEmitter.event().name("done").data("{}"));
            emitter.complete();

        } catch (Exception e) {
            log.error("[GROUP] 스트림 오류: roomId={}, duration={}ms, error={}",
                    roomId, System.currentTimeMillis() - streamStart, e.getMessage(), e);
            try {
                emitter.send(SseEmitter.event().name("error")
                        .data("{\"error\": \"응답 생성에 실패했습니다.\"}"));
            } catch (Exception ignored) {}
            emitter.completeWithError(e);
        }
    }

    // ── 내부 헬퍼 ─────────────────────────────────────────────────────────────

    @Transactional
    GroupMessage saveMessage(String roomId, String content, String senderCharacterId, String senderName) {
        GroupMessage msg = GroupMessage.builder()
                .id(UUID.randomUUID().toString())
                .roomId(roomId)
                .content(content)
                .senderCharacterId(senderCharacterId)
                .senderName(senderName)
                .createdAt(LocalDateTime.now())
                .build();
        return groupMessageRepository.save(msg);
    }

    @Transactional
    void updateRoomLastMessage(GroupRoom room, String message) {
        room.setLastMessage(message);
        room.setLastMessageAt(LocalDateTime.now());
        groupRoomRepository.save(room);
    }

    /**
     * 그룹 대화 맥락을 시스템 프롬프트에 추가합니다.
     * 다른 캐릭터들의 존재와 이름을 알려줍니다.
     */
    private String buildGroupContext(GroupRoom room, Character self, List<GroupMessage> history,
                                     Map<String, Character> characterMap) {
        List<String> otherNames = room.getCharacterIds().stream()
                .filter(id -> !id.equals(self.getId()))
                .map(id -> {
                    Character c = characterMap.get(id);
                    return c != null ? c.getName() : null;
                })
                .filter(name -> name != null)
                .collect(Collectors.toList());

        StringBuilder ctx = new StringBuilder("\n\n[그룹 대화 설정]");
        ctx.append("\n- 지금 여러 명이 함께 대화 중입니다.");
        if (!otherNames.isEmpty()) {
            ctx.append("\n- 같이 있는 친구들: ").append(String.join(", ", otherNames));
        }
        ctx.append("\n- 자신의 이름(").append(self.getName()).append(")으로 자연스럽게 참여해줘.");
        ctx.append("\n- 다른 친구들의 발언을 참고하되 자신의 개성을 유지해.");
        ctx.append("\n- 응답은 1~3문장으로 간결하게.");
        return ctx.toString();
    }

    /**
     * GroupMessage 리스트를 OllamaService가 사용하는 Message 리스트로 변환합니다.
     * 해당 캐릭터의 메시지는 assistant, 나머지는 user로 매핑합니다.
     */
    private List<Message> toOllamaHistory(List<GroupMessage> history, String characterId) {
        return history.stream()
                .map(gm -> Message.builder()
                        .id(gm.getId())
                        .content(gm.isUser()
                                ? gm.getContent()
                                : "[" + gm.getSenderName() + "]: " + gm.getContent())
                        .user(gm.isUser() || !gm.getSenderCharacterId().equals(characterId))
                        .createdAt(gm.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}
