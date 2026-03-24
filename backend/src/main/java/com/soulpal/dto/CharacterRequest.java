package com.soulpal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CharacterRequest {
    @NotBlank
    private String projectId;

    @NotBlank
    private String name;

    @NotBlank
    private String relationshipId;

    @NotNull
    private List<String> personalityIds;

    @NotNull
    private List<String> speechStyleIds;

    @NotNull
    private List<String> interestIds;

    @NotNull
    private List<String> appearanceIds;
}
