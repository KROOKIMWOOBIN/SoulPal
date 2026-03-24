package com.soulpal.repository;

import com.soulpal.model.GroupRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupRoomRepository extends JpaRepository<GroupRoom, String> {
    List<GroupRoom> findAllByUserIdAndProjectIdOrderByLastMessageAtDescCreatedAtDesc(String userId, String projectId);
    List<GroupRoom> findAllByUserIdOrderByLastMessageAtDescCreatedAtDesc(String userId);
    Optional<GroupRoom> findByIdAndUserId(String id, String userId);
    void deleteAllByUserId(String userId);
}
