package com.soulpal.repository;

import com.soulpal.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, String> {
    List<Project> findAllByUserIdOrderByCreatedAtDesc(String userId);
    Optional<Project> findByIdAndUserId(String id, String userId);
    void deleteAllByUserId(String userId);
}
