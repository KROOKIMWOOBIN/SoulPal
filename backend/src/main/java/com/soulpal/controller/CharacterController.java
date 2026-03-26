package com.soulpal.controller;

import com.soulpal.dto.CharacterRequest;
import com.soulpal.model.Character;
import com.soulpal.service.CharacterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/characters")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;

    @GetMapping
    public Page<Character> getAll(
            @RequestParam String projectId,
            @RequestParam(defaultValue = "recent") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        return characterService.getAll((String) auth.getPrincipal(), projectId, sort, page, size);
    }

    @GetMapping("/{id}")
    public Character getById(@PathVariable String id, Authentication auth) {
        return characterService.getById(id, (String) auth.getPrincipal());
    }

    @PostMapping
    public Character create(@Valid @RequestBody CharacterRequest req, Authentication auth) {
        return characterService.create((String) auth.getPrincipal(), req);
    }

    @PutMapping("/{id}")
    public Character update(@PathVariable String id, @Valid @RequestBody CharacterRequest req, Authentication auth) {
        return characterService.update(id, (String) auth.getPrincipal(), req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, Authentication auth) {
        characterService.delete(id, (String) auth.getPrincipal());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/favorite")
    public Character toggleFavorite(@PathVariable String id, Authentication auth) {
        return characterService.toggleFavorite(id, (String) auth.getPrincipal());
    }
}
