package com.soulpal.repository;

import com.soulpal.model.GroupMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupMessageRepository extends JpaRepository<GroupMessage, String> {
    List<GroupMessage> findByRoomIdOrderByCreatedAtDesc(String roomId, Pageable pageable);
    List<GroupMessage> findByRoomIdOrderByCreatedAtAsc(String roomId);
    void deleteByRoomId(String roomId);
    long countByRoomId(String roomId);
    Optional<GroupMessage> findFirstByRoomIdAndSenderCharacterIdIsNotNullOrderByCreatedAtDesc(String roomId);
}
