package com.soulpal.controller;

import com.soulpal.dto.CharacterRequest;
import com.soulpal.model.Character;
import com.soulpal.service.CharacterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/characters")
@RequiredArgsConstructor
public class CharacterController {

    private final CharacterService characterService;

    @GetMapping
    public List<Character> getAll(
            @RequestParam String projectId,
            @RequestParam(defaultValue = "recent") String sort) {
        return characterService.getAll(projectId, sort);
    }

    @GetMapping("/{id}")
    public Character getById(@PathVariable String id) {
        return characterService.getById(id);
    }

    @PostMapping
    public Character create(@Valid @RequestBody CharacterRequest req) {
        return characterService.create(req);
    }

    @PutMapping("/{id}")
    public Character update(@PathVariable String id, @Valid @RequestBody CharacterRequest req) {
        return characterService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        characterService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/favorite")
    public Character toggleFavorite(@PathVariable String id) {
        return characterService.toggleFavorite(id);
    }
}
