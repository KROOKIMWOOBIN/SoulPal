package com.soulpal.controller;

import com.soulpal.constants.CategoryData;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @GetMapping
    public Map<String, Object> getAll() {
        return Map.of(
            "relationships", CategoryData.RELATIONSHIPS,
            "personalities", CategoryData.PERSONALITIES,
            "speechStyles", CategoryData.SPEECH_STYLES,
            "interests", CategoryData.INTERESTS,
            "appearances", CategoryData.APPEARANCES
        );
    }
}
