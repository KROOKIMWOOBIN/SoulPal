package com.soulpal.service;

import com.soulpal.dto.ProjectRequest;
import com.soulpal.exception.BusinessException;
import com.soulpal.exception.ErrorCode;
import com.soulpal.model.Project;
import com.soulpal.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    public List<Project> getAll(String userId) {
        return projectRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    public Project getById(String id, String userId) {
        return projectRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
    }

    public Project create(ProjectRequest req, String userId) {
        Project project = Project.builder()
                .id(UUID.randomUUID().toString())
                .name(req.getName())
                .description(req.getDescription())
                .userId(userId)
                .build();
        return projectRepository.save(project);
    }

    public Project update(String id, ProjectRequest req, String userId) {
        Project project = getById(id, userId);
        project.setName(req.getName());
        project.setDescription(req.getDescription());
        return projectRepository.save(project);
    }

    public void delete(String id, String userId) {
        Project project = getById(id, userId);
        projectRepository.delete(project);
    }
}
