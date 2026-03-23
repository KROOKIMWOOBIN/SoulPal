package com.soulpal.repository;

import com.soulpal.model.Character;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CharacterRepository extends JpaRepository<Character, String> {
    // projectId로 필터링
    List<Character> findAllByUserIdAndProjectIdOrderByLastMessageAtDescCreatedAtDesc(String userId, String projectId);
    List<Character> findAllByUserIdAndProjectIdOrderByNameAsc(String userId, String projectId);
    List<Character> findAllByUserIdAndProjectIdAndFavoriteTrueOrderByLastMessageAtDesc(String userId, String projectId);

    // 프로젝트 없는 캐릭터 (하위호환)
    List<Character> findAllByUserIdOrderByLastMessageAtDescCreatedAtDesc(String userId);

    Optional<Character> findByIdAndUserId(String id, String userId);
}
