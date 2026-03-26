package com.soulpal.service;

import com.soulpal.exception.ResourceNotFoundException;
import com.soulpal.model.GroupMessage;
import com.soulpal.model.GroupRoom;
import com.soulpal.repository.GroupMessageRepository;
import com.soulpal.repository.GroupRoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GroupChatService 테스트")
class GroupChatServiceTest {

    @Mock GroupRoomRepository    groupRoomRepository;
    @Mock GroupMessageRepository groupMessageRepository;
    @Mock CharacterService       characterService;
    @Mock OllamaService          ollamaService;

    @InjectMocks GroupChatService groupChatService;

    private GroupRoom testRoom;
    private GroupMessage testMessage;

    @BeforeEach
    void setUp() {
        testRoom = GroupRoom.builder()
                .id("room-1")
                .userId("user-1")
                .projectId("proj-1")
                .name("테스트 방")
                .characterIds(List.of("char-1", "char-2"))
                .createdAt(LocalDateTime.now())
                .build();

        testMessage = GroupMessage.builder()
                .id("gmsg-1")
                .roomId("room-1")
                .content("안녕!")
                .senderCharacterId(null) // 유저 메시지
                .senderName("나")
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ── createRoom ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("그룹방 생성 → 캐릭터 소유권 검증 후 저장")
    void createRoom_success() {
        willDoNothing().given(characterService).verifyOwnership(anyString(), eq("user-1"));
        given(groupRoomRepository.save(any())).willReturn(testRoom);

        GroupRoom result = groupChatService.createRoom("user-1", "proj-1", "테스트 방", List.of("char-1", "char-2"));

        assertThat(result.getName()).isEqualTo("테스트 방");
        assertThat(result.getUserId()).isEqualTo("user-1");
        then(characterService).should().verifyOwnership("char-1", "user-1");
        then(characterService).should().verifyOwnership("char-2", "user-1");
        then(groupRoomRepository).should().save(any());
    }

    @Test
    @DisplayName("소유하지 않은 캐릭터로 방 생성 → ResourceNotFoundException")
    void createRoom_invalidCharacter() {
        willThrow(new ResourceNotFoundException("캐릭터를 찾을 수 없습니다"))
                .given(characterService).verifyOwnership("char-x", "user-1");

        assertThatThrownBy(() ->
                groupChatService.createRoom("user-1", "proj-1", "방", List.of("char-x")))
                .isInstanceOf(ResourceNotFoundException.class);

        then(groupRoomRepository).should(never()).save(any());
    }

    // ── getRooms ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("프로젝트 ID 있으면 프로젝트 필터링 조회")
    void getRooms_withProjectId() {
        given(groupRoomRepository.findAllByUserIdAndProjectIdOrderByLastMessageAtDescCreatedAtDesc("user-1", "proj-1"))
                .willReturn(List.of(testRoom));

        List<GroupRoom> result = groupChatService.getRooms("user-1", "proj-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProjectId()).isEqualTo("proj-1");
    }

    @Test
    @DisplayName("프로젝트 ID 없으면 전체 조회")
    void getRooms_withoutProjectId() {
        given(groupRoomRepository.findAllByUserIdOrderByLastMessageAtDescCreatedAtDesc("user-1"))
                .willReturn(List.of(testRoom));

        List<GroupRoom> result = groupChatService.getRooms("user-1", null);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("projectId 빈 문자열이면 전체 조회")
    void getRooms_blankProjectId() {
        given(groupRoomRepository.findAllByUserIdOrderByLastMessageAtDescCreatedAtDesc("user-1"))
                .willReturn(List.of(testRoom));

        List<GroupRoom> result = groupChatService.getRooms("user-1", "   ");

        assertThat(result).hasSize(1);
    }

    // ── getRoom ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("방 조회 성공")
    void getRoom_success() {
        given(groupRoomRepository.findByIdAndUserId("room-1", "user-1"))
                .willReturn(Optional.of(testRoom));

        GroupRoom result = groupChatService.getRoom("room-1", "user-1");

        assertThat(result.getId()).isEqualTo("room-1");
    }

    @Test
    @DisplayName("존재하지 않는 방 → ResourceNotFoundException")
    void getRoom_notFound() {
        given(groupRoomRepository.findByIdAndUserId("ghost", "user-1"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> groupChatService.getRoom("ghost", "user-1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── deleteRoom ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("방 삭제 → 메시지 먼저 삭제 후 방 삭제")
    void deleteRoom_success() {
        given(groupRoomRepository.findByIdAndUserId("room-1", "user-1"))
                .willReturn(Optional.of(testRoom));

        groupChatService.deleteRoom("room-1", "user-1");

        then(groupMessageRepository).should().deleteByRoomId("room-1");
        then(groupRoomRepository).should().delete(testRoom);
    }

    @Test
    @DisplayName("다른 유저 방 삭제 → ResourceNotFoundException, delete 미호출")
    void deleteRoom_wrongUser() {
        given(groupRoomRepository.findByIdAndUserId("room-1", "other-user"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> groupChatService.deleteRoom("room-1", "other-user"))
                .isInstanceOf(ResourceNotFoundException.class);

        then(groupRoomRepository).should(never()).delete(any());
    }

    // ── getMessages ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("메시지 조회 → 소유권 검증 후 asc 순서 반환")
    void getMessages_success() {
        GroupMessage msg1 = GroupMessage.builder()
                .id("m1").roomId("room-1").content("첫번째")
                .senderName("나").createdAt(LocalDateTime.now().minusMinutes(2)).build();
        GroupMessage msg2 = GroupMessage.builder()
                .id("m2").roomId("room-1").content("두번째")
                .senderName("나").createdAt(LocalDateTime.now()).build();

        given(groupRoomRepository.findByIdAndUserId("room-1", "user-1"))
                .willReturn(Optional.of(testRoom));
        // repository returns desc: msg2, msg1
        given(groupMessageRepository.findByRoomIdOrderByCreatedAtDesc(eq("room-1"), any()))
                .willReturn(List.of(msg2, msg1));

        List<GroupMessage> result = groupChatService.getMessages("room-1", "user-1", 0, 30);

        // service reverses to asc: msg1, msg2
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo("m1");
        assertThat(result.get(1).getId()).isEqualTo("m2");
    }

    @Test
    @DisplayName("다른 유저 방 메시지 조회 → ResourceNotFoundException")
    void getMessages_wrongUser() {
        given(groupRoomRepository.findByIdAndUserId("room-1", "other"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> groupChatService.getMessages("room-1", "other", 0, 30))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── saveMessage (package-private) ─────────────────────────────────────────

    @Test
    @DisplayName("메시지 저장 → 올바른 필드로 저장")
    void saveMessage_success() {
        given(groupMessageRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        GroupMessage saved = groupChatService.saveMessage("room-1", "안녕!", null, "나");

        assertThat(saved.getRoomId()).isEqualTo("room-1");
        assertThat(saved.getContent()).isEqualTo("안녕!");
        assertThat(saved.getSenderCharacterId()).isNull();
        assertThat(saved.getSenderName()).isEqualTo("나");
        assertThat(saved.isUser()).isTrue(); // senderCharacterId == null → isUser()
        assertThat(saved.getId()).isNotNull();
    }
}
