package com.soulpal.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {
    @NotBlank
    private String characterId;

    @NotBlank
    private String message;

    private int historyCount = 10;
}
