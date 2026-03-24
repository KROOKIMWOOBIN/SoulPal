package com.soulpal.controller;

import com.soulpal.dto.GroupChatRequest;
import com.soulpal.dto.GroupRoomRequest;
import com.soulpal.model.GroupMessage;
import com.soulpal.model.GroupRoom;
import com.soulpal.service.GroupChatService;
import com.soulpal.service.RateLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Tag(name = "GroupChat", description = "그룹 대화 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GroupChatController {

    private final GroupChatService groupChatService;
    private final RateLimitService rateLimitService;

    private final ExecutorService executor = new ThreadPoolExecutor(
            5, 20, 60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(100),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    // ── 대화방 관리 ────────────────────────────────────────────────────────────

    @Operation(summary = "그룹 대화방 생성")
    @PostMapping("/group-rooms")
    public ResponseEntity<GroupRoom> createRoom(
            @Valid @RequestBody GroupRoomRequest req, Authentication auth) {
        String userId = (String) auth.getPrincipal();
        GroupRoom room = groupChatService.createRoom(
                userId, req.getProjectId(), req.getName(), req.getCharacterIds());
        return ResponseEntity.status(HttpStatus.CREATED).body(room);
    }

    @Operation(summary = "그룹 대화방 목록 조회")
    @GetMapping("/group-rooms")
    public List<GroupRoom> getRooms(
            @RequestParam(required = false) String projectId, Authentication auth) {
        return groupChatService.getRooms((String) auth.getPrincipal(), projectId);
    }

    @Operation(summary = "그룹 대화방 단건 조회")
    @GetMapping("/group-rooms/{roomId}")
    public GroupRoom getRoom(@PathVariable String roomId, Authentication auth) {
        return groupChatService.getRoom(roomId, (String) auth.getPrincipal());
    }

    @Operation(summary = "그룹 대화방 삭제")
    @DeleteMapping("/group-rooms/{roomId}")
    public ResponseEntity<Void> deleteRoom(@PathVariable String roomId, Authentication auth) {
        groupChatService.deleteRoom(roomId, (String) auth.getPrincipal());
        return ResponseEntity.noContent().build();
    }

    // ── 메시지 ─────────────────────────────────────────────────────────────────

    @Operation(summary = "그룹 메시지 목록 조회 (페이징)")
    @GetMapping("/group-chat/messages/{roomId}")
    public List<GroupMessage> getMessages(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            Authentication auth) {
        return groupChatService.getMessages(roomId, (String) auth.getPrincipal(), page, size);
    }

    @Operation(summary = "그룹 SSE 스트리밍 채팅")
    @PostMapping(value = "/group-chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamGroupChat(
            @Valid @RequestBody GroupChatRequest req, Authentication auth) {
        String userId = (String) auth.getPrincipal();
        rateLimitService.checkChat(userId);

        SseEmitter emitter = new SseEmitter(300_000L); // 그룹 채팅은 5분 타임아웃

        executor.execute(() ->
                groupChatService.streamGroupChat(req.getRoomId(), userId, req.getMessage(), emitter));

        return emitter;
    }
}
