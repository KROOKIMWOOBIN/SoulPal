package com.soulpal.service;

import com.soulpal.dto.CharacterRequest;
import com.soulpal.exception.ResourceNotFoundException;
import com.soulpal.model.Character;
import com.soulpal.repository.CharacterRepository;
import com.soulpal.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CharacterService 테스트")
class CharacterServiceTest {

    @Mock CharacterRepository characterRepository;
    @Mock MessageRepository   messageRepository;

    @InjectMocks CharacterService characterService;

    private Character testCharacter;

    @BeforeEach
    void setUp() {
        testCharacter = Character.builder()
                .id("char-1")
                .userId("user-1")
                .projectId("proj-1")
                .name("소울")
                .build();
    }

    // SecurityContext stub 헬퍼
    private MockedStatic<SecurityContextHolder> stubUserId(String userId) {
        Authentication auth = mock(Authentication.class);
        given(auth.getPrincipal()).willReturn(userId);
        SecurityContext ctx = mock(SecurityContext.class);
        given(ctx.getAuthentication()).willReturn(auth);
        MockedStatic<SecurityContextHolder> mocked = mockStatic(SecurityContextHolder.class);
        mocked.when(SecurityContextHolder::getContext).thenReturn(ctx);
        return mocked;
    }

    // ── getById ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("캐릭터 조회 성공")
    void getById_success() {
        try (MockedStatic<SecurityContextHolder> ignored = stubUserId("user-1")) {
            given(characterRepository.findByIdAndUserId("char-1", "user-1"))
                    .willReturn(Optional.of(testCharacter));

            Character result = characterService.getById("char-1");

            assertThat(result.getId()).isEqualTo("char-1");
        }
    }

    @Test
    @DisplayName("존재하지 않는 캐릭터 → ResourceNotFoundException")
    void getById_notFound() {
        try (MockedStatic<SecurityContextHolder> ignored = stubUserId("user-1")) {
            given(characterRepository.findByIdAndUserId("ghost", "user-1"))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> characterService.getById("ghost"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── verifyOwnership ────────────────────────────────────────────────────────

    @Test
    @DisplayName("소유자 검증 성공 → 예외 없음")
    void verifyOwnership_success() {
        given(characterRepository.findByIdAndUserId("char-1", "user-1"))
                .willReturn(Optional.of(testCharacter));

        assertThatNoException().isThrownBy(
                () -> characterService.verifyOwnership("char-1", "user-1"));
    }

    @Test
    @DisplayName("다른 유저 소유 캐릭터 → ResourceNotFoundException")
    void verifyOwnership_wrongUser() {
        given(characterRepository.findByIdAndUserId("char-1", "other-user"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> characterService.verifyOwnership("char-1", "other-user"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("캐릭터 생성 성공 → 저장된 캐릭터 반환")
    void create_success() {
        try (MockedStatic<SecurityContextHolder> ignored = stubUserId("user-1")) {
            CharacterRequest req = new CharacterRequest();
            req.setProjectId("proj-1");
            req.setName("소울");
            req.setInterestIds(List.of());

            given(characterRepository.save(any())).willReturn(testCharacter);

            Character result = characterService.create(req);

            assertThat(result.getName()).isEqualTo("소울");
            then(characterRepository).should().save(any());
        }
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("캐릭터 삭제 → 메시지 먼저 삭제 후 캐릭터 삭제")
    void delete_success() {
        try (MockedStatic<SecurityContextHolder> ignored = stubUserId("user-1")) {
            given(characterRepository.findByIdAndUserId("char-1", "user-1"))
                    .willReturn(Optional.of(testCharacter));

            characterService.delete("char-1");

            then(messageRepository).should().deleteByCharacterId("char-1");
            then(characterRepository).should().delete(testCharacter);
        }
    }

    // ── toggleFavorite ────────────────────────────────────────────────────────

    @Test
    @DisplayName("즐겨찾기 토글 → 상태 반전 후 저장")
    void toggleFavorite_success() {
        try (MockedStatic<SecurityContextHolder> ignored = stubUserId("user-1")) {
            testCharacter.setFavorite(false);
            given(characterRepository.findByIdAndUserId("char-1", "user-1"))
                    .willReturn(Optional.of(testCharacter));
            given(characterRepository.save(any())).willReturn(testCharacter);

            Character result = characterService.toggleFavorite("char-1");

            assertThat(result.isFavorite()).isTrue();
        }
    }
}
