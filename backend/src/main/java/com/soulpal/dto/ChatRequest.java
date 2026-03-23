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

    /** true: 강제 웹 검색 / false: 키워드 자동 판단 */
    private boolean webSearch = false;
}
