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

    @NotBlank
    private String personalityId;

    @NotBlank
    private String speechStyleId;

    @NotNull
    private List<String> interestIds;

    @NotBlank
    private String appearanceId;
}
