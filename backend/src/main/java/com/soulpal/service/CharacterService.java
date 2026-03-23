package com.soulpal.service;

import com.soulpal.constants.CategoryData;
import com.soulpal.dto.CharacterRequest;
import com.soulpal.model.Character;
import com.soulpal.repository.CharacterRepository;
import com.soulpal.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CharacterService {

    private final CharacterRepository characterRepository;
    private final MessageRepository messageRepository;

    private String currentUserId() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public List<Character> getAll(String projectId, String sort) {
        String userId = currentUserId();
        return switch (sort) {
            case "name" -> characterRepository.findAllByUserIdAndProjectIdOrderByNameAsc(userId, projectId);
            case "favorite" -> characterRepository.findAllByUserIdAndProjectIdAndFavoriteTrueOrderByLastMessageAtDesc(userId, projectId);
            default -> characterRepository.findAllByUserIdAndProjectIdOrderByLastMessageAtDescCreatedAtDesc(userId, projectId);
        };
    }

    public Character getById(String id) {
        String userId = currentUserId();
        return characterRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("캐릭터를 찾을 수 없습니다: " + id));
    }

    @Transactional
    public Character create(CharacterRequest req) {
        String userId = currentUserId();
        Character character = Character.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .projectId(req.getProjectId())
                .name(req.getName())
                .relationshipId(req.getRelationshipId())
                .personalityId(req.getPersonalityId())
                .speechStyleId(req.getSpeechStyleId())
                .interestIds(req.getInterestIds())
                .appearanceId(req.getAppearanceId())
                .createdAt(LocalDateTime.now())
                .build();
        return characterRepository.save(character);
    }

    @Transactional
    public Character update(String id, CharacterRequest req) {
        Character character = getById(id);
        character.setName(req.getName());
        character.setRelationshipId(req.getRelationshipId());
        character.setPersonalityId(req.getPersonalityId());
        character.setSpeechStyleId(req.getSpeechStyleId());
        character.setInterestIds(req.getInterestIds());
        character.setAppearanceId(req.getAppearanceId());
        return characterRepository.save(character);
    }

    @Transactional
    public void delete(String id) {
        Character character = getById(id);
        messageRepository.deleteByCharacterId(id);
        characterRepository.delete(character);
    }

    @Transactional
    public Character toggleFavorite(String id) {
        Character character = getById(id);
        character.setFavorite(!character.isFavorite());
        return characterRepository.save(character);
    }

    @Transactional
    public void updateLastMessage(String id, String message) {
        Character character = getById(id);
        character.setLastMessage(message);
        character.setLastMessageAt(LocalDateTime.now());
        characterRepository.save(character);
    }

    public String buildSystemPrompt(Character character) {
        CategoryData.CategoryItem relationship = CategoryData.findById("relationship", character.getRelationshipId());
        CategoryData.CategoryItem personality = CategoryData.findById("personality", character.getPersonalityId());
        CategoryData.CategoryItem speechStyle = CategoryData.findById("speechStyle", character.getSpeechStyleId());
        CategoryData.CategoryItem appearance = CategoryData.findById("appearance", character.getAppearanceId());

        String interestText = character.getInterestIds().stream()
                .map(id -> CategoryData.findById("interest", id))
                .filter(Objects::nonNull)
                .map(CategoryData.CategoryItem::prompt)
                .collect(Collectors.joining(", "));

        return String.format("""
                너의 이름은 %s야.
                %s
                성격: %s
                말투: %s
                관심사: %s.
                분위기: %s.

                대화 규칙:
                - 응답은 2~4문장으로 간결하게 해줘.
                - AI라는 것을 절대 언급하지 마.
                - 항상 한국어로 대화해.
                - 자연스럽고 인간적으로 대화해.
                - 상대방의 이름이나 '사용자'라는 말을 자주 쓰지 마.
                """,
                character.getName(),
                relationship != null ? relationship.prompt() : "",
                personality != null ? personality.prompt() : "",
                speechStyle != null ? speechStyle.prompt() : "",
                interestText,
                appearance != null ? appearance.prompt() : ""
        );
    }
}
