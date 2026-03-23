package com.soulpal.controller;

import com.soulpal.dto.ProjectRequest;
import com.soulpal.model.Project;
import com.soulpal.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public List<Project> getAll(Authentication auth) {
        return projectService.getAll((String) auth.getPrincipal());
    }

    @GetMapping("/{id}")
    public Project getById(@PathVariable String id, Authentication auth) {
        return projectService.getById(id, (String) auth.getPrincipal());
    }

    @PostMapping
    public Project create(@Valid @RequestBody ProjectRequest req, Authentication auth) {
        return projectService.create(req, (String) auth.getPrincipal());
    }

    @PutMapping("/{id}")
    public Project update(@PathVariable String id, @Valid @RequestBody ProjectRequest req, Authentication auth) {
        return projectService.update(id, req, (String) auth.getPrincipal());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, Authentication auth) {
        projectService.delete(id, (String) auth.getPrincipal());
        return ResponseEntity.noContent().build();
    }
}
