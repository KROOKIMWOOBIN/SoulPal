package com.soulpal.repository;

import com.soulpal.model.Character;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CharacterRepository extends JpaRepository<Character, String> {
    // projectId로 필터링 (페이지네이션)
    Page<Character> findAllByUserIdAndProjectIdOrderByLastMessageAtDescCreatedAtDesc(String userId, String projectId, Pageable pageable);
    Page<Character> findAllByUserIdAndProjectIdOrderByNameAsc(String userId, String projectId, Pageable pageable);
    Page<Character> findAllByUserIdAndProjectIdAndFavoriteTrueOrderByLastMessageAtDesc(String userId, String projectId, Pageable pageable);

    // 프로젝트 없는 캐릭터 (하위호환)
    List<Character> findAllByUserIdOrderByLastMessageAtDescCreatedAtDesc(String userId);

    Optional<Character> findByIdAndUserId(String id, String userId);
    List<Character> findAllByUserId(String userId);
    List<Character> findAllByIdIn(List<String> ids);
    void deleteAllByUserId(String userId);
}
