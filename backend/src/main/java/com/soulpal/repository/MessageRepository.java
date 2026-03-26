package com.soulpal.repository;

import com.soulpal.model.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, String> {
    List<Message> findByCharacterIdOrderByCreatedAtDesc(String characterId, Pageable pageable);
    List<Message> findByCharacterIdOrderByCreatedAtAsc(String characterId);
    void deleteByCharacterId(String characterId);
    void deleteByCharacterIdIn(List<String> characterIds);
    long countByCharacterId(String characterId);

    @Query("SELECT m FROM Message m WHERE m.characterId = :characterId AND LOWER(m.content) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY m.createdAt DESC")
    List<Message> searchByContent(String characterId, String keyword);
}
